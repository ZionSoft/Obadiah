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
    public interface OnTranslationDownloadListener {
        public void onTranslationDownloaded(String translation, boolean isSuccessful);

        public void onTranslationDownloadProgress(String translation, int progress);
    }

    @Inject
    DatabaseHelper databaseHelper;

    private final Context context;

    @Inject
    public TranslationManager(Context context) {
        App.get(context).getInjectionComponent().inject(this);
        this.context = context.getApplicationContext();
    }

    public Translations loadTranslations(boolean forceRefresh) {
        if (!forceRefresh) {
            final List<TranslationInfo> translations;
            final List<String> downloaded;
            SQLiteDatabase db = null;
            try {
                db = databaseHelper.openDatabase();
                if (db != null) {
                    translations = TranslationHelper.sortByLocale(TranslationHelper.getTranslations(db));
                    downloaded = TranslationHelper.getDownloadedTranslationShortNames(db);
                    if (translations.size() > 0) {
                        return new Translations.Builder()
                                .translations(translations).downloaded(downloaded)
                                .build();
                    }
                } else {
                    Analytics.trackException("Failed to open database.");
                }
            } finally {
                if (db != null) {
                    databaseHelper.closeDatabase();
                }
            }
        }

        final long timestamp = SystemClock.elapsedRealtime();

        List<TranslationInfo> translations = null;
        try {
            translations = downloadTranslationList(NetworkHelper.PRIMARY_TRANSLATIONS_LIST_URL);
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
        }
        if (translations == null) {
            try {
                translations = downloadTranslationList(NetworkHelper.SECONDARY_TRANSLATIONS_LIST_URL);
            } catch (Exception e) {
                Crashlytics.getInstance().core.logException(e);
                Analytics.trackTranslationListDownloading(false, SystemClock.elapsedRealtime() - timestamp);
                return null;
            }
        }
        translations = TranslationHelper.sortByLocale(translations);

        final List<String> downloaded;
        SQLiteDatabase db = null;
        try {
            db = databaseHelper.openDatabase();
            if (db == null) {
                Analytics.trackException("Failed to open database.");
                Analytics.trackTranslationListDownloading(false, SystemClock.elapsedRealtime() - timestamp);
                return null;
            }
            TranslationHelper.saveTranslations(db, translations);
            downloaded = TranslationHelper.getDownloadedTranslationShortNames(db);
        } finally {
            if (db != null) {
                databaseHelper.closeDatabase();
            }
        }

        Analytics.trackTranslationListDownloading(true, SystemClock.elapsedRealtime() - timestamp);

        return new Translations.Builder().translations(translations).downloaded(downloaded).build();
    }

    private static List<TranslationInfo> downloadTranslationList(String url) throws Exception {
        return TranslationHelper.toTranslationList(new JSONArray(new String(NetworkHelper.get(url), "UTF8")));
    }

    public boolean removeTranslation(TranslationInfo translation) {
        final boolean removed = removeTranslation(translation.shortName);
        Analytics.trackTranslationRemoval(translation.shortName, removed);
        return removed;
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
                    Crashlytics.getInstance().core.logException(e);
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
                        Crashlytics.getInstance().core.logException(e);
                        Crashlytics.getInstance().core.logException(e);
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
