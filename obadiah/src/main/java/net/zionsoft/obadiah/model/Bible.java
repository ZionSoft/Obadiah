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

package net.zionsoft.obadiah.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.translations.TranslationHelper;
import net.zionsoft.obadiah.utils.SimpleAsyncTask;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Bible {
    public interface OnStringsLoadedListener {
        public void onStringsLoaded(List<String> strings);
    }

    public interface OnVersesLoadedListener {
        public void onVersesLoaded(List<Verse> verses);
    }

    private static final int BOOK_COUNT = 66;
    private static final int OLD_TESTAMENT_COUNT = 39;
    private static final int NEW_TESTAMENT_COUNT = 27;
    private static final int TOTAL_CHAPTER_COUNT = 1189;
    private static final int[] CHAPTER_COUNT = {50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36,
            10, 13, 10, 42, 150, 31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
            28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22};

    @Inject
    DatabaseHelper databaseHelper;

    private final LruCache<String, List<String>> bookNameCache;
    private final LruCache<String, List<Verse>> verseCache;

    @Inject
    public Bible(Context context) {
        super();
        App.get(context).getInjectionComponent().inject(this);

        final long maxMemory = Runtime.getRuntime().maxMemory();
        bookNameCache = new LruCache<String, List<String>>((int) (maxMemory / 16L)) {
            @Override
            protected int sizeOf(String key, List<String> texts) {
                // strings are UTF-16 encoded (with a length of one or two 16-bit code units)
                int length = 0;
                for (String text : texts)
                    length += text.length() * 4;
                return length;
            }
        };
        verseCache = new LruCache<String, List<Verse>>((int) (maxMemory / 8L)) {
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
        bookNameCache.evictAll();
        verseCache.evictAll();
    }

    public void loadDownloadedTranslations(final OnStringsLoadedListener listener) {
        new SimpleAsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... params) {
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

            @Override
            protected void onPostExecute(List<String> result) {
                listener.onStringsLoaded(Collections.unmodifiableList(result));
            }
        }.start();
    }

    @Nullable
    public List<String> loadBookNames(String translationShortName) {
        List<String> bookNames = bookNameCache.get(translationShortName);
        if (bookNames == null) {
            SQLiteDatabase db = null;
            try {
                db = databaseHelper.openDatabase();
                if (db == null) {
                    Analytics.trackException("Failed to open database.");
                    return null;
                }
                bookNames = Collections.unmodifiableList(
                        TranslationHelper.getBookNames(db, translationShortName));
            } finally {
                if (db != null) {
                    databaseHelper.closeDatabase();
                }
            }

            bookNameCache.put(translationShortName, bookNames);
        }
        return bookNames;
    }

    public void loadBookNames(final String translationShortName, final OnStringsLoadedListener listener) {
        new SimpleAsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... params) {
                return loadBookNames(translationShortName);
            }

            @Override
            protected void onPostExecute(List<String> result) {
                listener.onStringsLoaded(result);
            }
        }.start();
    }

    public void loadVerses(final String translationShortName, final int book, final int chapter,
                           final OnVersesLoadedListener listener) {
        final String key = buildVersesCacheKey(translationShortName, book, chapter);
        final List<Verse> verses = verseCache.get(key);
        if (verses != null) {
            listener.onVersesLoaded(verses);
            return;
        }

        new SimpleAsyncTask<Void, Void, List<Verse>>() {
            @Override
            protected List<Verse> doInBackground(Void... params) {
                SQLiteDatabase db = null;
                try {
                    db = databaseHelper.openDatabase();
                    if (db == null) {
                        Analytics.trackException("Failed to open database.");
                        return null;
                    }

                    List<String> bookNames = bookNameCache.get(translationShortName);
                    if (bookNames == null) {
                        // this should not happen, but just in case
                        bookNames = Collections.unmodifiableList(TranslationHelper.getBookNames(db, translationShortName));
                        bookNameCache.put(translationShortName, bookNames);
                    }

                    return TranslationHelper.getVerses(db, translationShortName,
                            bookNames.get(book), book, chapter);
                } finally {
                    if (db != null) {
                        databaseHelper.closeDatabase();
                    }
                }
            }

            @Override
            protected void onPostExecute(List<Verse> result) {
                result = Collections.unmodifiableList(result);
                verseCache.put(key, result);
                listener.onVersesLoaded(result);
            }
        }.start();
    }

    private static String buildVersesCacheKey(String translationShortName, int book, int chapter) {
        return String.format("%s-%d-%d", translationShortName, book, chapter);
    }

    public void searchVerses(final String translationShortName, final String keyword,
                             final OnVersesLoadedListener listener) {
        new SimpleAsyncTask<Void, Void, List<Verse>>() {
            @Override
            protected List<Verse> doInBackground(Void... params) {
                SQLiteDatabase db = null;
                try {
                    db = databaseHelper.openDatabase();
                    if (db == null) {
                        Analytics.trackException("Failed to open database.");
                        return null;
                    }
                    List<String> bookNames = bookNameCache.get(translationShortName);
                    if (bookNames == null) {
                        // this should not happen, but just in case
                        bookNames = Collections.unmodifiableList(TranslationHelper.getBookNames(db, translationShortName));
                        bookNameCache.put(translationShortName, bookNames);
                    }
                    return TranslationHelper.searchVerses(db, translationShortName, bookNames, keyword);
                } finally {
                    if (db != null) {
                        databaseHelper.closeDatabase();
                    }
                }
            }

            @Override
            protected void onPostExecute(List<Verse> result) {
                listener.onVersesLoaded(result);
            }
        }.start();
    }
}
