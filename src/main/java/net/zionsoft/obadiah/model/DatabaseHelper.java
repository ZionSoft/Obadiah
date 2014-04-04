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
import android.database.sqlite.SQLiteOpenHelper;

class DatabaseHelper extends SQLiteOpenHelper {
    static final String TABLE_BOOK_NAMES = "TABLE_BOOK_NAMES";
    private static final String INDEX_TABLE_BOOK_NAMES = "INDEX_TABLE_BOOK_NAMES";

    static final String COLUMN_TRANSLATION_SHORT_NAME = "COLUMN_TRANSLATION_SHORTNAME";
    static final String COLUMN_BOOK_INDEX = "COLUMN_BOOK_INDEX";
    static final String COLUMN_CHAPTER_INDEX = "COLUMN_CHAPTER_INDEX";
    static final String COLUMN_VERSE_INDEX = "COLUMN_VERSE_INDEX";
    static final String COLUMN_BOOK_NAME = "COLUMN_BOOK_NAME";
    static final String COLUMN_TEXT = "COLUMN_TEXT";

    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "DB_OBADIAH";

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL(String.format("CREATE TABLE %s (%s TEXT NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL);",
                    TABLE_BOOK_NAMES, COLUMN_TRANSLATION_SHORT_NAME,
                    COLUMN_BOOK_INDEX, COLUMN_BOOK_NAME));
            db.execSQL(String.format("CREATE INDEX %s ON %s (%s);",
                    INDEX_TABLE_BOOK_NAMES, TABLE_BOOK_NAMES, COLUMN_TRANSLATION_SHORT_NAME));

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            if (oldVersion < 2)
                db.execSQL("DROP TABLE IF EXISTS TABLE_TRANSLATIONS");

            if (oldVersion < 3)
                db.execSQL("DROP TABLE IF EXISTS TABLE_TRANSLATION_LIST");

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
