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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
    private static final int TOTAL_CHAPTER_COUNT = 1189;
    private static final int[] CHAPTER_COUNT = {50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36,
            10, 13, 10, 42, 150, 31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
            28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22};

    private static Bible sInstance;

    private final DatabaseHelper mDatabaseHelper;

    private final LruCache<String, List<String>> mBookNameCache;
    private final LruCache<String, List<Verse>> mVerseCache;
    private List<String> mDownloadedTranslationShortNames;
    private List<TranslationInfo> mDownloadedTranslations;
    private List<TranslationInfo> mAvailableTranslations;

    public static void initialize(Context context) {
        if (sInstance == null) {
            synchronized (Bible.class) {
                if (sInstance == null)
                    sInstance = new Bible(context.getApplicationContext());
            }
        }
    }

    private Bible(Context context) {
        super();

        mDatabaseHelper = DatabaseHelper.getInstance(context);

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

    public static Bible getInstance() {
        return sInstance;
    }

    public static int getBookCount() {
        return BOOK_COUNT;
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
        if (!forceRefresh && mDownloadedTranslations != null && mAvailableTranslations != null)
            listener.onTranslationsLoaded(mDownloadedTranslations, mAvailableTranslations);

        new AsyncTask<Void, Void, List<TranslationInfo>[]>() {
            private final long mTimestamp = SystemClock.elapsedRealtime();

            @Override
            protected List<TranslationInfo>[] doInBackground(Void... params) {
                try {
                    if (mDownloadedTranslationShortNames == null) {
                        // this should not happen, but just in case
                        mDownloadedTranslationShortNames = Collections.unmodifiableList(getDownloadedTranslations());
                    }

                    final JSONArray replyArray = new JSONArray(
                            new String(NetworkHelper.get(NetworkHelper.TRANSLATIONS_LIST_URL), "UTF8"));
                    final int length = replyArray.length();
                    final List<TranslationInfo> downloaded = new ArrayList<TranslationInfo>(mDownloadedTranslationShortNames.size());
                    final List<TranslationInfo> available = new ArrayList<TranslationInfo>(length - mDownloadedTranslationShortNames.size());
                    for (int i = 0; i < length; ++i) {
                        final JSONObject translationObject = replyArray.getJSONObject(i);
                        final String name = translationObject.getString("name");
                        final String shortName = translationObject.getString("shortName");
                        final String language = translationObject.getString("language");
                        final int size = translationObject.getInt("size");
                        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(shortName)
                                || TextUtils.isEmpty(language) || size <= 0) {
                            throw new Exception("Illegal translation info.");
                        }
                        final TranslationInfo translationInfo = new TranslationInfo(name, shortName, language, size);

                        boolean isDownloaded = false;
                        for (String translationShortName : mDownloadedTranslationShortNames) {
                            if (translationInfo.shortName.equals(translationShortName)) {
                                isDownloaded = true;
                                break;
                            }
                        }
                        if (isDownloaded)
                            downloaded.add(translationInfo);
                        else
                            available.add(translationInfo);
                    }

                    Collections.sort(available, new Comparator<TranslationInfo>() {
                        @Override
                        public int compare(TranslationInfo translation1, TranslationInfo translation2) {
                            // first compares with user's default locale
                            final Locale userLocale = Locale.getDefault();
                            final String userLanguage = userLocale.getLanguage().toLowerCase();
                            final String userCountry = userLocale.getCountry().toLowerCase();
                            final String[] fields1 = translation1.language.split("_");
                            final String[] fields2 = translation2.language.split("_");
                            final int score1 = compareLocale(fields1[0], fields1[1],
                                    userLanguage, userCountry);
                            final int score2 = compareLocale(fields2[0], fields2[1],
                                    userLanguage, userCountry);
                            int r = score2 - score1;
                            if (r != 0)
                                return r;

                            // then sorts by language & name
                            r = translation1.language.compareTo(translation2.language);
                            return r == 0 ? translation1.name.compareTo(translation2.name) : r;
                        }
                    });

                    return new List[]{downloaded, available};
                } catch (Exception e) {
                    Analytics.trackException("Failed to load translations - " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(List<TranslationInfo>[] result) {
                final boolean isSuccessful = result != null && result.length == 2;
                Analytics.trackTranslationListDownloading(isSuccessful, SystemClock.elapsedRealtime() - mTimestamp);

                if (isSuccessful) {
                    mDownloadedTranslations = result[0];
                    mAvailableTranslations = result[1];
                } else {
                    mDownloadedTranslations = null;
                    mAvailableTranslations = null;
                }
                listener.onTranslationsLoaded(mDownloadedTranslations, mAvailableTranslations);
            }
        }.execute();
    }

    private List<String> getDownloadedTranslations() {
        synchronized (mDatabaseHelper) {
            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = mDatabaseHelper.getReadableDatabase();
                if (db == null)
                    return null;
                cursor = db.query(true, DatabaseHelper.TABLE_BOOK_NAMES,
                        new String[]{DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME},
                        null, null, null, null, null, null);
                final int translationShortName = cursor.getColumnIndex(
                        DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME);
                final List<String> translations = new ArrayList<String>(cursor.getCount());
                while (cursor.moveToNext())
                    translations.add(cursor.getString(translationShortName));
                return translations;
            } finally {
                if (cursor != null)
                    cursor.close();
                if (db != null)
                    db.close();
            }
        }
    }

    private static int compareLocale(String language, String country, String targetLanguage, String targetCountry) {
        if (language.equals(targetLanguage))
            return (country.equals(targetCountry)) ? 2 : 1;
        return 0;
    }

    public void loadDownloadedTranslations(final OnStringsLoadedListener listener) {
        if (mDownloadedTranslationShortNames != null) {
            listener.onStringsLoaded(mDownloadedTranslationShortNames);
            return;
        }

        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... params) {
                return getDownloadedTranslations();
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
                synchronized (mDatabaseHelper) {
                    SQLiteDatabase db = null;
                    try {
                        db = mDatabaseHelper.getReadableDatabase();
                        if (db == null)
                            return null;
                        return getBookNames(db, translationShortName);
                    } finally {
                        if (db != null)
                            db.close();
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

    private static List<String> getBookNames(SQLiteDatabase db, String translationShortName) {
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.TABLE_BOOK_NAMES,
                    new String[]{DatabaseHelper.COLUMN_BOOK_NAME},
                    String.format("%s = ?", DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME),
                    new String[]{translationShortName}, null, null,
                    String.format("%s ASC", DatabaseHelper.COLUMN_BOOK_INDEX));
            final int bookName = cursor.getColumnIndex(DatabaseHelper.COLUMN_BOOK_NAME);
            final List<String> bookNames = new ArrayList<String>(BOOK_COUNT);
            while (cursor.moveToNext())
                bookNames.add(cursor.getString(bookName));
            return bookNames;
        } finally {
            if (cursor != null)
                cursor.close();
        }
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
                synchronized (mDatabaseHelper) {
                    SQLiteDatabase db = null;
                    Cursor cursor = null;
                    try {
                        db = mDatabaseHelper.getReadableDatabase();
                        if (db == null)
                            return null;

                        List<String> bookNames = mBookNameCache.get(translationShortName);
                        if (bookNames == null) {
                            // this should not happen, but just in case
                            bookNames = Collections.unmodifiableList(getBookNames(db, translationShortName));
                            mBookNameCache.put(translationShortName, bookNames);
                        }
                        final String bookName = bookNames.get(book);

                        cursor = db.query(translationShortName,
                                new String[]{DatabaseHelper.COLUMN_TEXT},
                                String.format("%s = ? AND %s = ?",
                                        DatabaseHelper.COLUMN_BOOK_INDEX, DatabaseHelper.COLUMN_CHAPTER_INDEX),
                                new String[]{Integer.toString(book), Integer.toString(chapter)},
                                null, null, String.format("%s ASC", DatabaseHelper.COLUMN_VERSE_INDEX)
                        );
                        final int verse = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT);
                        final List<Verse> verses = new ArrayList<Verse>(cursor.getCount());
                        int verseIndex = 0;
                        while (cursor.moveToNext())
                            verses.add(new Verse(book, chapter, verseIndex++, bookName, cursor.getString(verse)));
                        return verses;
                    } finally {
                        if (cursor != null)
                            cursor.close();
                        if (db != null)
                            db.close();
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
                synchronized (mDatabaseHelper) {
                    SQLiteDatabase db = null;
                    Cursor cursor = null;
                    try {
                        db = mDatabaseHelper.getReadableDatabase();
                        if (db == null)
                            return null;
                        cursor = db.query(translationShortName,
                                new String[]{DatabaseHelper.COLUMN_BOOK_INDEX, DatabaseHelper.COLUMN_CHAPTER_INDEX,
                                        DatabaseHelper.COLUMN_VERSE_INDEX, DatabaseHelper.COLUMN_TEXT},
                                String.format("%s LIKE ?", DatabaseHelper.COLUMN_TEXT),
                                new String[]{String.format("%%%s%%", keyword.trim().replaceAll("\\s+", "%"))},
                                null, null, null
                        );
                        final int count = cursor.getCount();
                        if (count == 0)
                            return Collections.EMPTY_LIST;

                        List<String> bookNames = mBookNameCache.get(translationShortName);
                        if (bookNames == null) {
                            // this should not happen, but just in case
                            bookNames = Collections.unmodifiableList(getBookNames(db, translationShortName));
                            mBookNameCache.put(translationShortName, bookNames);
                        }

                        final int bookIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_BOOK_INDEX);
                        final int chapterIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CHAPTER_INDEX);
                        final int verseIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_VERSE_INDEX);
                        final int verseText = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT);
                        final List<Verse> verses = new ArrayList<Verse>(count);
                        while (cursor.moveToNext()) {
                            final int book = cursor.getInt(bookIndex);
                            verses.add(new Verse(book, cursor.getInt(chapterIndex), cursor.getInt(verseIndex),
                                    bookNames.get(book), cursor.getString(verseText)));
                        }
                        return verses;
                    } finally {
                        if (cursor != null)
                            cursor.close();
                        if (db != null)
                            db.close();
                    }
                }
            }

            @Override
            protected void onPostExecute(List<Verse> result) {
                listener.onVersesLoaded(result);
            }
        }.execute();
    }

    public void downloadTranslation(final String translationShortName, final OnTranslationDownloadListener listener) {
        new AsyncTask<Void, Integer, Boolean>() {
            private final long mTimestamp = SystemClock.elapsedRealtime();

            @Override
            protected Boolean doInBackground(Void... params) {
                synchronized (mDatabaseHelper) {
                    ZipInputStream zis = null;
                    SQLiteDatabase db = null;
                    try {
                        db = mDatabaseHelper.getWritableDatabase();
                        if (db == null)
                            return false;
                        db.beginTransaction();

                        // creates a translation table
                        db.execSQL(String.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL);",
                                translationShortName, DatabaseHelper.COLUMN_BOOK_INDEX, DatabaseHelper.COLUMN_CHAPTER_INDEX,
                                DatabaseHelper.COLUMN_VERSE_INDEX, DatabaseHelper.COLUMN_TEXT));
                        db.execSQL(String.format("CREATE INDEX INDEX_%s ON %s (%s, %s, %s);",
                                translationShortName, translationShortName, DatabaseHelper.COLUMN_BOOK_INDEX,
                                DatabaseHelper.COLUMN_CHAPTER_INDEX, DatabaseHelper.COLUMN_VERSE_INDEX));

                        zis = new ZipInputStream(NetworkHelper.getStream(String.format(
                                NetworkHelper.TRANSLATION_URL_TEMPLATE, URLEncoder.encode(translationShortName, "UTF-8"))));

                        final byte buffer[] = new byte[2048];
                        final ByteArrayOutputStream os = new ByteArrayOutputStream();
                        final ContentValues versesValues = new ContentValues(4);
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
                                // writes the book names table

                                final ContentValues bookNamesValues = new ContentValues(3);
                                bookNamesValues.put(DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME, translationShortName);

                                final JSONObject booksInfoObject = new JSONObject(new String(bytes, "UTF8"));
                                final JSONArray booksArray = booksInfoObject.getJSONArray("books");
                                for (int i = 0; i < Bible.getBookCount(); ++i) {
                                    bookNamesValues.put(DatabaseHelper.COLUMN_BOOK_INDEX, i);

                                    final String bookName = booksArray.getString(i);
                                    if (TextUtils.isEmpty(bookName))
                                        throw new Exception("Illegal books.json file: " + translationShortName);
                                    bookNamesValues.put(DatabaseHelper.COLUMN_BOOK_NAME, bookName);

                                    db.insert(DatabaseHelper.TABLE_BOOK_NAMES, null, bookNamesValues);
                                }
                            } else {
                                // writes the verses

                                final String[] parts = fileName.split("-");
                                final int bookIndex = Integer.parseInt(parts[0]);
                                final int chapterIndex = Integer.parseInt(parts[1]);
                                versesValues.put(DatabaseHelper.COLUMN_BOOK_INDEX, bookIndex);
                                versesValues.put(DatabaseHelper.COLUMN_CHAPTER_INDEX, chapterIndex);

                                final JSONObject jsonObject = new JSONObject(new String(bytes, "UTF8"));
                                final JSONArray paragraphArray = jsonObject.getJSONArray("verses");
                                final int paragraphCount = paragraphArray.length();
                                boolean hasNonEmptyVerse = false;
                                for (int verseIndex = 0; verseIndex < paragraphCount; ++verseIndex) {
                                    versesValues.put(DatabaseHelper.COLUMN_VERSE_INDEX, verseIndex);

                                    final String verse = paragraphArray.getString(verseIndex);
                                    if (!hasNonEmptyVerse && !TextUtils.isEmpty(verse))
                                        hasNonEmptyVerse = true;
                                    versesValues.put(DatabaseHelper.COLUMN_TEXT, verse);

                                    db.insert(translationShortName, null, versesValues);
                                }
                                if (!hasNonEmptyVerse) {
                                    throw new Exception(String.format("Empty chapter: %s %d-%d",
                                            translationShortName, bookIndex, chapterIndex));
                                }
                            }

                            // broadcasts progress
                            publishProgress(++downloaded / 12);
                        }

                        db.setTransactionSuccessful();

                        return true;
                    } catch (Exception e) {
                        Analytics.trackException("Failed to download translations - " + e.getMessage());
                        return false;
                    } finally {
                        if (db != null) {
                            if (db.inTransaction())
                                db.endTransaction();
                            db.close();
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

            @Override
            protected void onProgressUpdate(Integer... values) {
                listener.onTranslationDownloadProgress(translationShortName, values[0]);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                Analytics.trackTranslationDownload(translationShortName, result, SystemClock.elapsedRealtime() - mTimestamp);

                mDownloadedTranslationShortNames = unmodifiableAppend(mDownloadedTranslationShortNames, translationShortName);

                TranslationInfo downloaded = null;
                for (TranslationInfo available : mAvailableTranslations) {
                    if (available.shortName.equals(translationShortName)) {
                        downloaded = available;
                        break;
                    }
                }
                mDownloadedTranslations = unmodifiableAppend(mDownloadedTranslations, downloaded);
                mAvailableTranslations = unmodifiableRemove(mAvailableTranslations, downloaded);

                listener.onTranslationDownloaded(translationShortName, result);
            }
        }.execute();
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
                synchronized (mDatabaseHelper) {
                    SQLiteDatabase db = null;
                    try {
                        db = mDatabaseHelper.getWritableDatabase();
                        if (db == null)
                            return false;

                        db.beginTransaction();

                        // deletes the translation table
                        db.execSQL(String.format("DROP TABLE IF EXISTS %s", translationShortName));

                        // deletes the book names
                        db.delete(DatabaseHelper.TABLE_BOOK_NAMES,
                                String.format("%s = ?", DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME),
                                new String[]{translationShortName});

                        db.setTransactionSuccessful();

                        return true;
                    } finally {
                        if (db != null) {
                            if (db.inTransaction())
                                db.endTransaction();
                            db.close();
                        }
                    }
                }
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
}
