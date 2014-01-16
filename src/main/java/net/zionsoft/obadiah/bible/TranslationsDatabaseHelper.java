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

package net.zionsoft.obadiah.bible;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TranslationsDatabaseHelper extends SQLiteOpenHelper {
    public TranslationsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            createTranslationListTable(db);
            createBookNamesTable(db);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            if (oldVersion < 2) {
                // new format of the translation list table is introduced in version 2
                createTranslationListTable(db);
                db.execSQL("DROP TABLE IF EXISTS TABLE_TRANSLATIONS");
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void createTranslationListTable(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL);",
                TABLE_TRANSLATION_LIST, COLUMN_TRANSLATION_ID, COLUMN_KEY, COLUMN_VALUE));
    }

    private void createBookNamesTable(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE %s (%s TEXT NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL);",
                TABLE_BOOK_NAMES, COLUMN_TRANSLATION_SHORT_NAME, COLUMN_BOOK_INDEX, COLUMN_BOOK_NAME));
        db.execSQL(String.format("CREATE INDEX %s ON %s (%s);",
                INDEX_TABLE_BOOK_NAMES, TABLE_BOOK_NAMES, COLUMN_TRANSLATION_SHORT_NAME));
    }

    static final String TABLE_TRANSLATION_LIST = "TABLE_TRANSLATION_LIST";
    static final String TABLE_BOOK_NAMES = "TABLE_BOOK_NAMES";
    private static final String INDEX_TABLE_BOOK_NAMES = "INDEX_TABLE_BOOK_NAMES";

    static final String COLUMN_TRANSLATION_ID = "COLUMN_TRANSLATION_ID";
    static final String COLUMN_TRANSLATION_SHORT_NAME = "COLUMN_TRANSLATION_SHORTNAME";
    static final String COLUMN_BOOK_INDEX = "COLUMN_BOOK_INDEX";
    static final String COLUMN_CHAPTER_INDEX = "COLUMN_CHAPTER_INDEX";
    static final String COLUMN_VERSE_INDEX = "COLUMN_VERSE_INDEX";
    static final String COLUMN_BOOK_NAME = "COLUMN_BOOK_NAME";
    static final String COLUMN_TEXT = "COLUMN_TEXT";
    static final String COLUMN_KEY = "COLUMN_KEY";
    static final String COLUMN_VALUE = "COLUMN_VALUE";

    static final String KEY_NAME = "KEY_NAME";
    static final String KEY_SHORT_NAME = "KEY_SHORT_NAME";
    static final String KEY_LANGUAGE = "KEY_LANGUAGE";
    static final String KEY_BLOB_KEY = "KEY_BLOB_KEY";
    static final String KEY_SIZE = "KEY_SIZE";
    static final String KEY_TIMESTAMP = "KEY_TIMESTAMP";
    static final String KEY_INSTALLED = "KEY_INSTALLED";

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "DB_OBADIAH";
}
