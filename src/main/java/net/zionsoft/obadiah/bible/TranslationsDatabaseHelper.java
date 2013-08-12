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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TranslationsDatabaseHelper extends SQLiteOpenHelper {
    public TranslationsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            // creates the translations table
            db.execSQL(String.format("CREATE TABLE %s (%s TEXT NOT NULL, %s TEXT UNIQUE NOT NULL, %s TEXT NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL);",
                    TABLE_TRANSLATIONS, COLUMN_TRANSLATION_NAME, COLUMN_TRANSLATION_SHORTNAME,
                    COLUMN_LANGUAGE, COLUMN_DOWNLOAD_SIZE, COLUMN_INSTALLED));

            // creates the books name table
            db.execSQL(String.format("CREATE TABLE %s (%s TEXT NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL);",
                    TABLE_BOOK_NAMES, COLUMN_TRANSLATION_SHORTNAME,
                    COLUMN_BOOK_INDEX, COLUMN_BOOK_NAME));
            db.execSQL(String.format("CREATE INDEX %s ON %s (%s);",
                    INDEX_TABLE_BOOK_NAMES, TABLE_BOOK_NAMES, COLUMN_TRANSLATION_SHORTNAME));

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // does nothing
    }

    public static final String TABLE_BOOK_NAMES = "TABLE_BOOK_NAMES";
    public static final String TABLE_TRANSLATIONS = "TABLE_TRANSLATIONS";
    public static final String INDEX_TABLE_BOOK_NAMES = "INDEX_TABLE_BOOK_NAMES";
    public static final String COLUMN_TRANSLATION_NAME = "COLUMN_TRANSLATION_NAME";
    public static final String COLUMN_TRANSLATION_SHORTNAME = "COLUMN_TRANSLATION_SHORTNAME";
    public static final String COLUMN_LANGUAGE = "COLUMN_LANGUAGE";
    public static final String COLUMN_DOWNLOAD_SIZE = "COLUMN_DOWNLOAD_SIZE";
    public static final String COLUMN_INSTALLED = "COLUMN_INSTALLED";
    public static final String COLUMN_BOOK_NAME = "COLUMN_BOOK_NAME";
    public static final String COLUMN_BOOK_INDEX = "COLUMN_BOOK_INDEX";
    public static final String COLUMN_CHAPTER_INDEX = "COLUMN_CHAPTER_INDEX";
    public static final String COLUMN_VERSE_INDEX = "COLUMN_VERSE_INDEX";
    public static final String COLUMN_TEXT = "COLUMN_TEXT";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "DB_OBADIAH";
}
