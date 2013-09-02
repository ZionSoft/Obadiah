/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2013 ZionSoft
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

package net.zionsoft.obadiah.bible;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.LruCache;

import java.util.ArrayList;
import java.util.List;

public class TranslationReader {
    public static class SearchResult {
        public final int bookIndex;
        public final int chapterIndex;
        public final int verseIndex;
        public final String verse;

        public SearchResult(int bookIndex, int chapterIndex, int verseIndex, String verse) {
            super();
            this.bookIndex = bookIndex;
            this.chapterIndex = chapterIndex;
            this.verseIndex = verseIndex;
            this.verse = verse;
        }
    }

    public TranslationReader(Context context) {
        super();
        mTranslationsDatabaseHelper = new TranslationsDatabaseHelper(context);
        mSelectedTranslationChanged = true;
        mBookNames = new String[BOOK_COUNT];
    }

    public void selectTranslation(String translationShortName) {
        if (translationShortName == null)
            throw new IllegalArgumentException();

        if (translationShortName.equals(mSelectedTranslationShortName))
            return;

        // checks if this translation is installed
        SQLiteDatabase db = null;
        try {
            db = mTranslationsDatabaseHelper.getReadableDatabase();
            Cursor cursor = db.query(TranslationsDatabaseHelper.TABLE_TRANSLATION_LIST,
                    new String[]{TranslationsDatabaseHelper.COLUMN_TRANSLATION_ID},
                    String.format("%s = ? AND %s = ?",
                            TranslationsDatabaseHelper.COLUMN_KEY,
                            TranslationsDatabaseHelper.COLUMN_VALUE),
                    new String[]{TranslationsDatabaseHelper.KEY_SHORT_NAME, translationShortName},
                    null, null, null);
            if (cursor == null || cursor.getCount() != 1)
                throw new IllegalArgumentException();
            cursor.moveToFirst();
            final long uniqueId = cursor.getLong(
                    cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_TRANSLATION_ID));
            cursor = db.query(TranslationsDatabaseHelper.TABLE_TRANSLATION_LIST,
                    new String[]{TranslationsDatabaseHelper.COLUMN_VALUE},
                    String.format("%s = ? AND %s = ?",
                            TranslationsDatabaseHelper.COLUMN_TRANSLATION_ID,
                            TranslationsDatabaseHelper.COLUMN_KEY),
                    new String[]{Long.toString(uniqueId), TranslationsDatabaseHelper.KEY_INSTALLED},
                    null, null, null);
            if (cursor == null || cursor.getCount() != 1)
                throw new IllegalArgumentException();
        } finally {
            if (db != null)
                db.close();
        }

        mSelectedTranslationChanged = true;
        mSelectedTranslationShortName = translationShortName;
    }

    public String selectedTranslationShortName() {
        return mSelectedTranslationShortName;
    }

    public static int bookCount() {
        return BOOK_COUNT;
    }

    public static int chapterCount(int bookIndex) {
        if (bookIndex < 0 || bookIndex >= BOOK_COUNT)
            throw new IllegalArgumentException();
        return CHAPTER_COUNT[bookIndex];
    }

    public String[] bookNames() {
        if (mSelectedTranslationShortName == null)
            return null;

        if (!mSelectedTranslationChanged)
            return mBookNames;

        // loads the book names
        final SQLiteDatabase db = mTranslationsDatabaseHelper.getReadableDatabase();
        final Cursor cursor = db.query(TranslationsDatabaseHelper.TABLE_BOOK_NAMES,
                new String[]{TranslationsDatabaseHelper.COLUMN_BOOK_NAME},
                String.format("%s = ?", TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME),
                new String[]{mSelectedTranslationShortName}, null, null,
                String.format("%s ASC", TranslationsDatabaseHelper.COLUMN_BOOK_INDEX));
        if (cursor == null) {
            db.close();
            return null;
        }

        final int bookNameColumnIndex
                = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_BOOK_NAME);
        int i = 0;
        while (cursor.moveToNext())
            mBookNames[i++] = cursor.getString(bookNameColumnIndex);
        db.close();
        mSelectedTranslationChanged = false;
        return mBookNames;
    }

    public String[] verses(int bookIndex, int chapterIndex) {
        if (mSelectedTranslationShortName == null)
            return null;

        if (bookIndex < 0 || bookIndex >= BOOK_COUNT
                || chapterIndex < 0 || chapterIndex >= chapterCount(bookIndex)) {
            throw new IllegalArgumentException();
        }

        if (sVersesCache == null)
            createVersesCache();
        return loadVerses(bookIndex, chapterIndex);
    }

    public List<SearchResult> search(String key) throws IllegalArgumentException {
        if (mSelectedTranslationShortName == null)
            return null;
        if (key == null || key.length() == 0)
            throw new IllegalArgumentException();

        SQLiteDatabase db = null;
        try {
            db = mTranslationsDatabaseHelper.getReadableDatabase();
            final Cursor cursor = db.query(mSelectedTranslationShortName,
                    new String[]{TranslationsDatabaseHelper.COLUMN_BOOK_INDEX,
                            TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX,
                            TranslationsDatabaseHelper.COLUMN_VERSE_INDEX,
                            TranslationsDatabaseHelper.COLUMN_TEXT},
                    String.format("%s LIKE ?", TranslationsDatabaseHelper.COLUMN_TEXT),
                    new String[]{String.format("%%%s%%", key.trim().replaceAll("\\s+", "%"))},
                    null, null, null);
            if (cursor == null)
                return null;
            final int count = cursor.getCount();
            if (count == 0)
                return null;

            final int bookIndexColumnIndex
                    = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX);
            final int chapterIndexColumnIndex
                    = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX);
            final int verseIndexColumnIndex
                    = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_VERSE_INDEX);
            final int textColumnIndex
                    = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_TEXT);

            List<SearchResult> results = new ArrayList<SearchResult>(count);
            while (cursor.moveToNext()) {
                final int bookIndex = cursor.getInt(bookIndexColumnIndex);
                final int chapterIndex = cursor.getInt(chapterIndexColumnIndex);
                final int verseIndex = cursor.getInt(verseIndexColumnIndex);

                // format: <book name> <chapter index>:<verse index>\n<text>
                final String verse = String.format("%s %d:%d\n%s",
                        bookNames()[bookIndex], chapterIndex + 1, verseIndex + 1,
                        cursor.getString(textColumnIndex));

                results.add(new SearchResult(bookIndex, chapterIndex, verseIndex, verse));
            }
            return results;
        } finally {
            if (db != null)
                db.close();
        }
    }


    // memcache for verses

    private void createVersesCache() {
        // uses 1/8 of available memory for the cache
        final int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 8L);
        sVersesCache = new LruCache<String, String[]>(cacheSize) {
            @Override
            protected int sizeOf(String key, String[] verses) {
                // strings are UTF-16 encoded (with a length of one or two 16-bit code units)
                int length = 0;
                for (String verse : verses)
                    length += verse.length() * 4;
                return length;
            }
        };
    }

    private String[] loadVerses(int bookIndex, int chapterIndex) {
        final String key
                = buildVersesCacheKey(mSelectedTranslationShortName, bookIndex, chapterIndex);
        String[] verses = sVersesCache.get(key);
        if (verses == null) {
            SQLiteDatabase db = null;
            try {
                db = mTranslationsDatabaseHelper.getReadableDatabase();
                final Cursor cursor = db.query(mSelectedTranslationShortName,
                        new String[]{TranslationsDatabaseHelper.COLUMN_TEXT},
                        String.format("%s = ? AND %s = ?",
                                TranslationsDatabaseHelper.COLUMN_BOOK_INDEX,
                                TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX),
                        new String[]{Integer.toString(bookIndex), Integer.toString(chapterIndex)},
                        null, null, String.format("%s ASC", TranslationsDatabaseHelper.COLUMN_VERSE_INDEX));
                if (cursor == null)
                    return null;
                final int count = cursor.getCount();
                if (count == 0)
                    return null;

                final int textColumnIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_TEXT);
                verses = new String[count];
                int i = -1;
                while (cursor.moveToNext())
                    verses[++i] = cursor.getString(textColumnIndex);

                sVersesCache.put(key, verses);
            } finally {
                if (db != null)
                    db.close();
            }
        }
        return verses;
    }

    private static String buildVersesCacheKey(String translationShortName,
                                              int bookIndex, int chapterIndex) {
        return String.format("%s%d%d", translationShortName, bookIndex, chapterIndex);
    }


    private static final int BOOK_COUNT = 66;
    private static final int[] CHAPTER_COUNT = {50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29,
            36, 10, 13, 10, 42, 150, 31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2,
            14, 4, 28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1,
            1, 1, 22};

    private static LruCache<String, String[]> sVersesCache = null;

    private boolean mSelectedTranslationChanged;
    private String mSelectedTranslationShortName;
    private final String[] mBookNames;

    private final TranslationsDatabaseHelper mTranslationsDatabaseHelper;
}
