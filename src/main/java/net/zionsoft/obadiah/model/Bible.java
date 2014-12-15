/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2014 ZionSoft
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

package net.zionsoft.obadiah.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.util.LruCache;
import android.util.Pair;

import com.crashlytics.android.Crashlytics;

import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.network.NetworkHelper;
import net.zionsoft.obadiah.model.translations.TranslationHelper;

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
import javax.inject.Singleton;

@Singleton
public class Bible {
    public static interface OnStringsLoadedListener {
        public void onStringsLoaded(List<String> strings);
    }

    public static interface OnTranslationsLoadedListener {
        public void onTranslationsLoaded(List<TranslationInfo> downloaded, List<TranslationInfo> available);
    }

    public static interface OnTranslationDownloadListener {
        public void onTranslationDownloaded(String translation, boolean isSuccessful);

        public void onTranslationDownloadProgress(String translation, int progress);
    }

    public static interface OnTranslationRemovedListener {
        public void onTranslationRemoved(String translation, boolean isSuccessful);
    }

    public static interface OnVersesLoadedListener {
        public void onVersesLoaded(List<Verse> verses);
    }

    private static final int BOOK_COUNT = 66;
    private static final int OLD_TESTAMENT_COUNT = 39;
    private static final int NEW_TESTAMENT_COUNT = 27;
    private static final int TOTAL_CHAPTER_COUNT = 1189;
    private static final int[] CHAPTER_COUNT = {50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36,
            10, 13, 10, 42, 150, 31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
            28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22};

    private final Context mContext;

    private final LruCache<String, List<String>> mBookNameCache;
    private final LruCache<String, List<Verse>> mVerseCache;
    private List<String> mDownloadedTranslationShortNames;
    private List<TranslationInfo> mDownloadedTranslations;
    private List<TranslationInfo> mAvailableTranslations;

    @Inject
    public Bible(Context context) {
        super();

        mContext = context;

        final long maxMemory = Runtime.getRuntime().maxMemory();
        mBookNameCache = new LruCache<String, List<String>>((int) (maxMemory / 16L)) {
            @Override
            protected int sizeOf(String key, List<String> texts) {
                // strings are UTF-16 encoded (with a length of one or two 16-bit code units)
                int length = 0;
                for (String text : texts)
                    length += text.length() * 4;
                return length;
            }
        };
        mVerseCache = new LruCache<String, List<Verse>>((int) (maxMemory / 8L)) {
            @Override
            protected int sizeOf(String key, List<Verse> verses) {
                // each Verse contains 3 integers and 2 strings
                // strings are UTF-16 encoded (with a length of one or two 16-bit code units)
                int length = 0;
                for (Verse verse : verses)
                    length += 12 + (verse.bookName.length() + verse.verseText.length()) * 4;
                return length;
            }
        };
    }

    public static int getBookCount() {
        return BOOK_COUNT;
    }

    public static int getOldTestamentBookCount() {
        return OLD_TESTAMENT_COUNT;
    }

    public static int getNewTestamentBookCount() {
        return NEW_TESTAMENT_COUNT;
    }

    public static int getTotalChapterCount() {
        return TOTAL_CHAPTER_COUNT;
    }

    public static int getChapterCount(int book) {
        return CHAPTER_COUNT[book];
    }

    public void clearCache() {
        mBookNameCache.evictAll();
        mVerseCache.evictAll();
    }

    public void loadTranslations(boolean forceRefresh, final OnTranslationsLoadedListener listener) {
        if (!forceRefresh && mDownloadedTranslations != null && mAvailableTranslations != null) {
            listener.onTranslationsLoaded(mDownloadedTranslations, mAvailableTranslations);
            return;
        }

        if (!NetworkHelper.isOnline(mContext)) {
            // do nothing if network is not available
            listener.onTranslationsLoaded(null, null);
            return;
        }

        new AsyncTask<Void, Void, Pair<List<TranslationInfo>, List<TranslationInfo>>>() {
            private final long mTimestamp = SystemClock.elapsedRealtime();

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

                if (mDownloadedTranslationShortNames == null) {
                    // this should not happen, but just in case
                    mDownloadedTranslationShortNames = Collections.unmodifiableList(getDownloadedTranslationShortNames());
                }

                final List<TranslationInfo> downloaded
                        = new ArrayList<TranslationInfo>(mDownloadedTranslationShortNames.size());
                final List<TranslationInfo> available
                        = new ArrayList<TranslationInfo>(translations.size() - mDownloadedTranslationShortNames.size());
                translations = TranslationHelper.sortByLocale(translations);
                for (TranslationInfo translation : translations) {
                    boolean isDownloaded = false;
                    for (String translationShortName : mDownloadedTranslationShortNames) {
                        if (translation.shortName.equals(translationShortName)) {
                            isDownloaded = true;
                            break;
                        }
                    }
                    if (isDownloaded)
                        downloaded.add(translation);
                    else
                        available.add(translation);
                }

                return new Pair<List<TranslationInfo>, List<TranslationInfo>>(downloaded, available);
            }

            @Override
            protected void onPostExecute(Pair<List<TranslationInfo>, List<TranslationInfo>> result) {
                final boolean isSuccessful = result != null;
                Analytics.trackTranslationListDownloading(isSuccessful, SystemClock.elapsedRealtime() - mTimestamp);

                if (isSuccessful) {
                    mDownloadedTranslations = result.first;
                    mAvailableTranslations = result.second;
                } else {
                    mDownloadedTranslations = null;
                    mAvailableTranslations = null;
                }
                listener.onTranslationsLoaded(mDownloadedTranslations, mAvailableTranslations);
            }
        }.execute();
    }

    private List<String> getDownloadedTranslationShortNames() {
        SQLiteDatabase db = null;
        try {
            db = DatabaseHelper.openDatabase();
            if (db == null) {
                Analytics.trackException("Failed to open database.");
                return null;
            }
            return TranslationHelper.getDownloadedTranslationShortNames(db);
        } finally {
            if (db != null) {
                DatabaseHelper.closeDatabase();
            }
        }
    }

    private static List<TranslationInfo> downloadTranslationList(String url) throws Exception {
        return TranslationHelper.toTranslationList(new JSONArray(new String(NetworkHelper.get(url), "UTF8")));
    }

    public void loadDownloadedTranslations(final OnStringsLoadedListener listener) {
        if (mDownloadedTranslationShortNames != null) {
            listener.onStringsLoaded(mDownloadedTranslationShortNames);
            return;
        }

        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... params) {
                return getDownloadedTranslationShortNames();
            }

            @Override
            protected void onPostExecute(List<String> result) {
                mDownloadedTranslationShortNames = Collections.unmodifiableList(result);
                listener.onStringsLoaded(mDownloadedTranslationShortNames);
            }
        }.execute();
    }

    public void loadBookNames(final String translationShortName, final OnStringsLoadedListener listener) {
        final List<String> bookNames = mBookNameCache.get(translationShortName);
        if (bookNames != null) {
            listener.onStringsLoaded(bookNames);
            return;
        }

        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... params) {
                SQLiteDatabase db = null;
                try {
                    db = DatabaseHelper.openDatabase();
                    if (db == null) {
                        Analytics.trackException("Failed to open database.");
                        return null;
                    }
                    return TranslationHelper.getBookNames(db, translationShortName);
                } finally {
                    if (db != null) {
                        DatabaseHelper.closeDatabase();
                    }
                }
            }

            @Override
            protected void onPostExecute(List<String> result) {
                result = Collections.unmodifiableList(result);
                mBookNameCache.put(translationShortName, result);
                listener.onStringsLoaded(result);
            }
        }.execute();
    }

    public void loadVerses(final String translationShortName, final int book, final int chapter,
                           final OnVersesLoadedListener listener) {
        final String key = buildVersesCacheKey(translationShortName, book, chapter);
        final List<Verse> verses = mVerseCache.get(key);
        if (verses != null) {
            listener.onVersesLoaded(verses);
            return;
        }

        new AsyncTask<Void, Void, List<Verse>>() {
            @Override
            protected List<Verse> doInBackground(Void... params) {
                SQLiteDatabase db = null;
                try {
                    db = DatabaseHelper.openDatabase();
                    if (db == null) {
                        Analytics.trackException("Failed to open database.");
                        return null;
                    }

                    List<String> bookNames = mBookNameCache.get(translationShortName);
                    if (bookNames == null) {
                        // this should not happen, but just in case
                        bookNames = Collections.unmodifiableList(TranslationHelper.getBookNames(db, translationShortName));
                        mBookNameCache.put(translationShortName, bookNames);
                    }

                    return TranslationHelper.getVerses(db, translationShortName,
                            bookNames.get(book), book, chapter);
                } finally {
                    if (db != null) {
                        DatabaseHelper.closeDatabase();
                    }
                }
            }

            @Override
            protected void onPostExecute(List<Verse> result) {
                result = Collections.unmodifiableList(result);
                mVerseCache.put(key, result);
                listener.onVersesLoaded(result);
            }
        }.execute();
    }

    private static String buildVersesCacheKey(String translationShortName, int book, int chapter) {
        return String.format("%s-%d-%d", translationShortName, book, chapter);
    }

    public void searchVerses(final String translationShortName, final String keyword,
                             final OnVersesLoadedListener listener) {
        new AsyncTask<Void, Void, List<Verse>>() {
            @Override
            protected List<Verse> doInBackground(Void... params) {
                SQLiteDatabase db = null;
                try {
                    db = DatabaseHelper.openDatabase();
                    if (db == null) {
                        Analytics.trackException("Failed to open database.");
                        return null;
                    }
                    List<String> bookNames = mBookNameCache.get(translationShortName);
                    if (bookNames == null) {
                        // this should not happen, but just in case
                        bookNames = Collections.unmodifiableList(TranslationHelper.getBookNames(db, translationShortName));
                        mBookNameCache.put(translationShortName, bookNames);
                    }
                    return TranslationHelper.searchVerses(db, translationShortName, bookNames, keyword);
                } finally {
                    if (db != null) {
                        DatabaseHelper.closeDatabase();
                    }
                }
            }

            @Override
            protected void onPostExecute(List<Verse> result) {
                listener.onVersesLoaded(result);
            }
        }.execute();
    }

    public void downloadTranslation(final TranslationInfo translationInfo, final OnTranslationDownloadListener listener) {
        if (!NetworkHelper.isOnline(mContext)) {
            // do nothing if network is not available
            listener.onTranslationDownloaded(translationInfo.shortName, false);
            return;
        }

        new AsyncTask<Void, Integer, Boolean>() {
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

                mDownloadedTranslationShortNames = unmodifiableAppend(mDownloadedTranslationShortNames, translationInfo.shortName);

                TranslationInfo downloaded = null;
                for (TranslationInfo available : mAvailableTranslations) {
                    if (available.shortName.equals(translationInfo.shortName)) {
                        downloaded = available;
                        break;
                    }
                }
                mDownloadedTranslations = unmodifiableAppend(mDownloadedTranslations, downloaded);
                mAvailableTranslations = unmodifiableRemove(mAvailableTranslations, downloaded);

                listener.onTranslationDownloaded(translationInfo.shortName, result);
            }
        }.execute();
    }

    private static interface OnDownloadProgressListener {
        public void onProgress(int progress);
    }

    private void downloadTranslation(String url, String translationShortName,
                                     OnDownloadProgressListener onProgress) throws Exception {
        ZipInputStream zis = null;
        SQLiteDatabase db = null;
        try {
            db = DatabaseHelper.openDatabase();
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
                DatabaseHelper.closeDatabase();
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

    private static <T> List<T> unmodifiableAppend(List<T> original, T toAppend) {
        final List<T> list = new ArrayList<T>(original.size() + 1);
        list.addAll(original);
        list.add(toAppend);
        return Collections.unmodifiableList(list);
    }

    private static <T> List<T> unmodifiableRemove(List<T> original, T toRemove) {
        final List<T> list = new ArrayList<T>(original.size() - 1);
        for (T t : original) {
            if (!t.equals(toRemove))
                list.add(t);
        }
        return Collections.unmodifiableList(list);
    }

    public void removeTranslation(final String translationShortName, final OnTranslationRemovedListener listener) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return removeTranslation(translationShortName);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                Analytics.trackTranslationRemoval(translationShortName, result);

                mDownloadedTranslationShortNames = unmodifiableRemove(mDownloadedTranslationShortNames, translationShortName);

                TranslationInfo removed = null;
                for (TranslationInfo downloaded : mDownloadedTranslations) {
                    if (downloaded.shortName.equals(translationShortName)) {
                        removed = downloaded;
                        break;
                    }
                }
                mDownloadedTranslations = unmodifiableRemove(mDownloadedTranslations, removed);
                mAvailableTranslations = unmodifiableAppend(mAvailableTranslations, removed);

                listener.onTranslationRemoved(translationShortName, result);
            }
        }.execute();
    }

    private boolean removeTranslation(String translationShortName) {
        SQLiteDatabase db = null;
        try {
            db = DatabaseHelper.openDatabase();
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
                DatabaseHelper.closeDatabase();
            }
        }
    }
}
