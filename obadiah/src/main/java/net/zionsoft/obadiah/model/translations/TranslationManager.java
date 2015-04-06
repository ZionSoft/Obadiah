/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2015 ZionSoft
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.zionsoft.obadiah.model.translations;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.crashlytics.android.Crashlytics;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.network.NetworkHelper;
import net.zionsoft.obadiah.utils.SimpleAsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;

public class TranslationManager {
    public interface OnTranslationsLoadedListener {
        public void onTranslationsLoaded(@Nullable List<TranslationInfo> downloaded, @Nullable List<TranslationInfo> available);
    }

    public interface OnTranslationDownloadListener {
        public void onTranslationDownloaded(String translation, boolean isSuccessful);

        public void onTranslationDownloadProgress(String translation, int progress);
    }

    public interface OnTranslationRemovedListener {
        public void onTranslationRemoved(String translation, boolean isSuccessful);
    }

    @Inject
    DatabaseHelper databaseHelper;

    private final Context context;

    private List<String> downloadedTranslationShortNames;
    private List<TranslationInfo> downloadedTranslations;
    private List<TranslationInfo> availableTranslations;

    @Inject
    public TranslationManager(Context context) {
        App.get(context).getInjectionComponent().inject(this);
        this.context = context.getApplicationContext();
    }

    public void loadTranslations(boolean forceRefresh, final OnTranslationsLoadedListener listener) {
        if (!forceRefresh && downloadedTranslations != null && availableTranslations != null) {
            // we have cache, so use it
            listener.onTranslationsLoaded(downloadedTranslations, availableTranslations);
            return;
        }

        if (!NetworkHelper.isOnline(context)) {
            // do nothing if network is not available
            listener.onTranslationsLoaded(null, null);
            return;
        }

        final long timestamp = SystemClock.elapsedRealtime();
        new SimpleAsyncTask<Void, Void, Pair<List<TranslationInfo>, List<TranslationInfo>>>() {
            @Override
            protected Pair<List<TranslationInfo>, List<TranslationInfo>> doInBackground(Void... params) {
                List<TranslationInfo> translations = null;
                try {
                    translations = downloadTranslationList(NetworkHelper.PRIMARY_TRANSLATIONS_LIST_URL);
                } catch (Exception e) {
                    Crashlytics.logException(e);
                }
                if (translations == null) {
                    try {
                        translations = downloadTranslationList(NetworkHelper.SECONDARY_TRANSLATIONS_LIST_URL);
                    } catch (Exception e) {
                        Crashlytics.logException(e);
                        return null;
                    }
                }

                if (downloadedTranslationShortNames == null) {
                    // this should not happen, but just in case
                    downloadedTranslationShortNames = Collections.unmodifiableList(getDownloadedTranslationShortNames());
                }

                final List<TranslationInfo> downloaded
                        = new ArrayList<>(downloadedTranslationShortNames.size());
                final List<TranslationInfo> available
                        = new ArrayList<>(translations.size() - downloadedTranslationShortNames.size());
                translations = TranslationHelper.sortByLocale(translations);
                for (TranslationInfo translation : translations) {
                    boolean isDownloaded = false;
                    for (String translationShortName : downloadedTranslationShortNames) {
                        if (translation.shortName.equals(translationShortName)) {
                            isDownloaded = true;
                            break;
                        }
                    }
                    if (isDownloaded) {
                        downloaded.add(translation);
                    } else {
                        available.add(translation);
                    }
                }

                return new Pair<>(downloaded, available);
            }

            @Override
            protected void onPostExecute(Pair<List<TranslationInfo>, List<TranslationInfo>> result) {
                final boolean isSuccessful = result != null;
                Analytics.trackTranslationListDownloading(isSuccessful, SystemClock.elapsedRealtime() - timestamp);

                if (isSuccessful) {
                    downloadedTranslations = result.first;
                    availableTranslations = result.second;
                } else {
                    downloadedTranslations = null;
                    availableTranslations = null;
                }
                listener.onTranslationsLoaded(downloadedTranslations, availableTranslations);
            }
        }.start();
    }

    private static List<TranslationInfo> downloadTranslationList(String url) throws Exception {
        return TranslationHelper.toTranslationList(new JSONArray(new String(NetworkHelper.get(url), "UTF8")));
    }

    private List<String> getDownloadedTranslationShortNames() {
        SQLiteDatabase db = null;
        try {
            db = databaseHelper.openDatabase();
            if (db == null) {
                Analytics.trackException("Failed to open database.");
                return null;
            }
            return TranslationHelper.getDownloadedTranslationShortNames(db);
        } finally {
            if (db != null) {
                databaseHelper.closeDatabase();
            }
        }
    }

    public void removeTranslation(final String translationShortName, final OnTranslationRemovedListener listener) {
        new SimpleAsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return removeTranslation(translationShortName);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                Analytics.trackTranslationRemoval(translationShortName, result);

                downloadedTranslationShortNames = unmodifiableRemove(downloadedTranslationShortNames, translationShortName);

                TranslationInfo removed = null;
                for (TranslationInfo downloaded : downloadedTranslations) {
                    if (downloaded.shortName.equals(translationShortName)) {
                        removed = downloaded;
                        break;
                    }
                }
                downloadedTranslations = unmodifiableRemove(downloadedTranslations, removed);
                availableTranslations = unmodifiableAppend(availableTranslations, removed);

                listener.onTranslationRemoved(translationShortName, result);
            }
        }.start();
    }

    private boolean removeTranslation(String translationShortName) {
        SQLiteDatabase db = null;
        try {
            db = databaseHelper.openDatabase();
            if (db == null) {
                Analytics.trackException("Failed to open database.");
                return false;
            }
            db.beginTransaction();
            TranslationHelper.removeTranslation(db, translationShortName);
            db.setTransactionSuccessful();

            return true;
        } finally {
            if (db != null) {
                if (db.inTransaction()) {
                    db.endTransaction();
                }
                databaseHelper.closeDatabase();
            }
        }
    }

    private static <T> List<T> unmodifiableAppend(List<T> original, T toAppend) {
        final List<T> list = new ArrayList<>(original.size() + 1);
        list.addAll(original);
        list.add(toAppend);
        return Collections.unmodifiableList(list);
    }

    private static <T> List<T> unmodifiableRemove(List<T> original, T toRemove) {
        final List<T> list = new ArrayList<>(original.size() - 1);
        for (T t : original) {
            if (!t.equals(toRemove))
                list.add(t);
        }
        return Collections.unmodifiableList(list);
    }

    public void downloadTranslation(final TranslationInfo translationInfo, final OnTranslationDownloadListener listener) {
        if (!NetworkHelper.isOnline(context)) {
            // do nothing if network is not available
            listener.onTranslationDownloaded(translationInfo.shortName, false);
            return;
        }

        new SimpleAsyncTask<Void, Integer, Boolean>() {
            private final long mTimestamp = SystemClock.elapsedRealtime();

            @Override
            protected Boolean doInBackground(Void... params) {
                // before 1.7.0, there is a bug that set last read translation, so we try to remove
                // the translation first
                removeTranslation(translationInfo.shortName);

                final OnDownloadProgressListener onProgress = new OnDownloadProgressListener() {
                    @Override
                    public void onProgress(int progress) {
                        publishProgress(progress);
                    }
                };

                boolean downloaded = false;
                try {
                    final String url = String.format(NetworkHelper.PRIMARY_TRANSLATION_URL_TEMPLATE,
                            URLEncoder.encode(translationInfo.blobKey, "UTF-8"));
                    downloadTranslation(url, translationInfo.shortName, onProgress);
                    downloaded = true;
                } catch (Exception e) {
                    Crashlytics.logException(e);
                }

                if (!downloaded) {
                    try {
                        // TODO if downloading from primary server fails with a non-zero progress,
                        // it starts from zero again
                        final String url = String.format(NetworkHelper.SECONDARY_TRANSLATION_URL_TEMPLATE,
                                URLEncoder.encode(translationInfo.shortName, "UTF-8"));
                        downloadTranslation(url, translationInfo.shortName, onProgress);
                        downloaded = true;
                    } catch (Exception e) {
                        Crashlytics.logException(e);
                    }
                }
                return downloaded;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                listener.onTranslationDownloadProgress(translationInfo.shortName, values[0]);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                Analytics.trackTranslationDownload(translationInfo.shortName, result, SystemClock.elapsedRealtime() - mTimestamp);

                if (result) {
                    downloadedTranslationShortNames = unmodifiableAppend(downloadedTranslationShortNames, translationInfo.shortName);

                    TranslationInfo downloaded = null;
                    for (TranslationInfo available : availableTranslations) {
                        if (available.shortName.equals(translationInfo.shortName)) {
                            downloaded = available;
                            break;
                        }
                    }
                    downloadedTranslations = unmodifiableAppend(downloadedTranslations, downloaded);
                    availableTranslations = unmodifiableRemove(availableTranslations, downloaded);
                }
                listener.onTranslationDownloaded(translationInfo.shortName, result);
            }
        }.start();
    }

    private interface OnDownloadProgressListener {
        public void onProgress(int progress);
    }

    private void downloadTranslation(String url, String translationShortName,
                                     OnDownloadProgressListener onProgress) throws Exception {
        ZipInputStream zis = null;
        SQLiteDatabase db = null;
        try {
            db = databaseHelper.openDatabase();
            if (db == null) {
                Analytics.trackException("Failed to open database.");
                throw new Exception("Failed to open database for writing");
            }
            db.beginTransaction();

            TranslationHelper.createTranslationTable(db, translationShortName);

            zis = new ZipInputStream(NetworkHelper.getStream(url));

            final byte buffer[] = new byte[2048];
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            int downloaded = 0;
            int read;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                os.reset();
                while ((read = zis.read(buffer, 0, 2048)) != -1)
                    os.write(buffer, 0, read);
                final byte[] bytes = os.toByteArray();
                String fileName = entry.getName();
                fileName = fileName.substring(0, fileName.length() - 5); // removes the trailing ".json"
                if (fileName.equals("books")) {
                    TranslationHelper.saveBookNames(db, new JSONObject(new String(bytes, "UTF8")));
                } else {
                    final String[] parts = fileName.split("-");
                    final int bookIndex = Integer.parseInt(parts[0]);
                    final int chapterIndex = Integer.parseInt(parts[1]);
                    TranslationHelper.saveVerses(db, translationShortName,
                            bookIndex, chapterIndex, new JSONObject(new String(bytes, "UTF8")));
                }

                onProgress.onProgress(++downloaded / 12);
            }

            db.setTransactionSuccessful();
        } finally {
            if (db != null) {
                if (db.inTransaction()) {
                    db.endTransaction();
                }
                databaseHelper.closeDatabase();
            }
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                    // we can't do much here
                }
            }
        }
    }
}
