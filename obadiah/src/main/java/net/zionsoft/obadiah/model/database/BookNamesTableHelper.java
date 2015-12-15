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

package net.zionsoft.obadiah.model.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import net.zionsoft.obadiah.model.domain.Bible;
import net.zionsoft.obadiah.network.BackendTranslationInfo;

import java.util.ArrayList;
import java.util.List;

public class BookNamesTableHelper {
    private static final String TABLE_BOOK_NAMES = "TABLE_BOOK_NAMES";
    private static final String INDEX_TABLE_BOOK_NAMES = "INDEX_TABLE_BOOK_NAMES";
    private static final String COLUMN_TRANSLATION_SHORT_NAME = "COLUMN_TRANSLATION_SHORTNAME";
    private static final String COLUMN_BOOK_INDEX = "COLUMN_BOOK_INDEX";
    private static final String COLUMN_BOOK_NAME = "COLUMN_BOOK_NAME";

    static void createTable(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE %s (%s TEXT NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL);",
                TABLE_BOOK_NAMES, COLUMN_TRANSLATION_SHORT_NAME, COLUMN_BOOK_INDEX, COLUMN_BOOK_NAME));
        db.execSQL(String.format("CREATE INDEX %s ON %s (%s);",
                INDEX_TABLE_BOOK_NAMES, TABLE_BOOK_NAMES, COLUMN_TRANSLATION_SHORT_NAME));
    }

    @NonNull
    public static List<String> getBookNames(SQLiteDatabase db, String translationShortName) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_BOOK_NAMES, new String[]{COLUMN_BOOK_NAME},
                    String.format("%s = ?", COLUMN_TRANSLATION_SHORT_NAME),
                    new String[]{translationShortName}, null, null,
                    String.format("%s ASC", COLUMN_BOOK_INDEX));
            final int bookName = cursor.getColumnIndex(COLUMN_BOOK_NAME);
            final List<String> bookNames = new ArrayList<>(Bible.getBookCount());
            while (cursor.moveToNext()) {
                bookNames.add(cursor.getString(bookName));
            }
            return bookNames;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void saveBookNames(SQLiteDatabase db, BackendTranslationInfo translation) {
        final ContentValues bookNamesValues = new ContentValues(3);
        bookNamesValues.put(COLUMN_TRANSLATION_SHORT_NAME, translation.shortName);
        final List<String> books = translation.books;
        final int count = books.size();
        for (int i = 0; i < count; ++i) {
            bookNamesValues.put(COLUMN_BOOK_INDEX, i);
            bookNamesValues.put(COLUMN_BOOK_NAME, books.get(i));
            db.insert(TABLE_BOOK_NAMES, null, bookNamesValues);
        }
    }

    public static void removeBookNames(SQLiteDatabase db, String translationShortName) {
        db.delete(TABLE_BOOK_NAMES, String.format("%s = ?", COLUMN_TRANSLATION_SHORT_NAME),
                new String[]{translationShortName});
    }
}
