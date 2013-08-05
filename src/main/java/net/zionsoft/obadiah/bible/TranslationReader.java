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

public class TranslationReader {
    public TranslationReader(Context context) {
        super();
        mTranslationsDatabaseHelper = new TranslationsDatabaseHelper(context);
        mSelectedTranslationChanged = true;
        mBookNames = new String[BOOK_COUNT];
    }

    public void selectTranslation(String translationShortName) {
        final SQLiteDatabase db = mTranslationsDatabaseHelper.getReadableDatabase();
        Cursor cursor;
        if (translationShortName != null) {
            cursor = db.query(TranslationsDatabaseHelper.TABLE_TRANSLATIONS,
                    new String[]{TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME},
                    TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME + " = ? AND "
                            + TranslationsDatabaseHelper.COLUMN_INSTALLED + " = ?", new String[]{
                    translationShortName, "1"}, null, null, null);
        } else {
            // if the given name is null, choose the first installed translation
            cursor = db.query(TranslationsDatabaseHelper.TABLE_TRANSLATIONS,
                    new String[]{TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME},
                    TranslationsDatabaseHelper.COLUMN_INSTALLED + " = ?",
                    new String[]{"1"}, null, null, null, "1");
        }

        if (cursor == null || cursor.getCount() != 1) {
            db.close();
            throw new IllegalArgumentException();
        }

        if (translationShortName == null) {
            cursor.moveToNext();
            translationShortName = cursor.getString(cursor
                    .getColumnIndex(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME));
        }

        db.close();
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
                TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME + " = ?",
                new String[]{mSelectedTranslationShortName}, null, null,
                TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + " ASC");
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
        // TODO caches the verses in memory

        if (mSelectedTranslationShortName == null)
            return null;

        if (chapterIndex < 0 || chapterIndex >= chapterCount(bookIndex))
            throw new IllegalArgumentException();

        final SQLiteDatabase db = mTranslationsDatabaseHelper.getReadableDatabase();
        final Cursor cursor = db.query(mSelectedTranslationShortName,
                new String[]{TranslationsDatabaseHelper.COLUMN_TEXT},
                TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + " = ? AND "
                        + TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX + " = ?",
                new String[]{Integer.toString(bookIndex), Integer.toString(chapterIndex)},
                null, null, TranslationsDatabaseHelper.COLUMN_VERSE_INDEX + " ASC");
        if (cursor == null) {
            db.close();
            return null;
        }
        final int count = cursor.getCount();
        if (count == 0) {
            db.close();
            return null;
        }

        final int textColumnIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_TEXT);
        final String[] texts = new String[count];
        int i = 0;
        while (cursor.moveToNext())
            texts[i++] = cursor.getString(textColumnIndex);
        db.close();
        return texts;
    }

    private static final int BOOK_COUNT = 66;
    private static final int[] CHAPTER_COUNT = {50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29,
            36, 10, 13, 10, 42, 150, 31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2,
            14, 4, 28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1,
            1, 1, 22};

    private boolean mSelectedTranslationChanged;
    private String mSelectedTranslationShortName;
    private String[] mBookNames;

    private TranslationsDatabaseHelper mTranslationsDatabaseHelper;
}
