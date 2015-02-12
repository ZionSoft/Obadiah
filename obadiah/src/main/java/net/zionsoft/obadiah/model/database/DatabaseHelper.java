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
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String TABLE_BOOK_NAMES = "TABLE_BOOK_NAMES";
    public static final String TABLE_READING_PROGRESS = "TABLE_READING_PROGRESS";
    private static final String TABLE_METADATA = "TABLE_METADATA";
    private static final String INDEX_TABLE_BOOK_NAMES = "INDEX_TABLE_BOOK_NAMES";

    public static final String COLUMN_TRANSLATION_SHORT_NAME = "COLUMN_TRANSLATION_SHORTNAME";
    public static final String COLUMN_BOOK_INDEX = "COLUMN_BOOK_INDEX";
    public static final String COLUMN_CHAPTER_INDEX = "COLUMN_CHAPTER_INDEX";
    public static final String COLUMN_VERSE_INDEX = "COLUMN_VERSE_INDEX";
    public static final String COLUMN_BOOK_NAME = "COLUMN_BOOK_NAME";
    public static final String COLUMN_TEXT = "COLUMN_TEXT";
    public static final String COLUMN_LAST_READING_TIMESTAMP = "COLUMN_LAST_READING_TIMESTAMP";
    private static final String COLUMN_KEY = "COLUMN_KEY";
    private static final String COLUMN_VALUE = "COLUMN_VALUE";

    public static final String KEY_LAST_READING_TIMESTAMP = "KEY_LAST_READING_TIMESTAMP";
    public static final String KEY_CONTINUOUS_READING_DAYS = "KEY_CONTINUOUS_READING_DAYS";

    private static final int DATABASE_VERSION = 5;
    private static final String DATABASE_NAME = "DB_OBADIAH";

    private int mCounter;
    private SQLiteDatabase mDatabase;

    @Inject
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public SQLiteDatabase openDatabase() {
        synchronized (this) {
            if (++mCounter == 1) {
                mDatabase = getWritableDatabase();
            }
            return mDatabase;
        }
    }

    public void closeDatabase() {
        synchronized (this) {
            if (--mCounter == 0) {
                mDatabase.close();
                mDatabase = null;
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL(String.format("CREATE TABLE %s (%s TEXT NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL);",
                    TABLE_BOOK_NAMES, COLUMN_TRANSLATION_SHORT_NAME, COLUMN_BOOK_INDEX, COLUMN_BOOK_NAME));
            db.execSQL(String.format("CREATE INDEX %s ON %s (%s);",
                    INDEX_TABLE_BOOK_NAMES, TABLE_BOOK_NAMES, COLUMN_TRANSLATION_SHORT_NAME));

            createReadingProgressTable(db);
            createMetadataTable(db);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void createReadingProgressTable(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, PRIMARY KEY (%s, %s));",
                TABLE_READING_PROGRESS, COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_LAST_READING_TIMESTAMP,
                COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX));
    }

    private static void createMetadataTable(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE %s (%s TEXT PRIMARY KEY, %s TEXT NOT NULL);",
                TABLE_METADATA, COLUMN_KEY, COLUMN_VALUE));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            // version 2 introduced in 1.7.0
            if (oldVersion < 2)
                db.execSQL("DROP TABLE IF EXISTS TABLE_TRANSLATIONS");

            // version 3 introduced in 1.8.0
            if (oldVersion < 3)
                db.execSQL("DROP TABLE IF EXISTS TABLE_TRANSLATION_LIST");

            // version 4 introduced in 1.8.2
            if (oldVersion < 4)
                createReadingProgressTable(db);

            // version 5 introduced in 1.9.0
            if (oldVersion < 5)
                createMetadataTable(db);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static void setMetadata(SQLiteDatabase db, String key, String value) {
        final ContentValues values = new ContentValues(2);
        values.put(COLUMN_KEY, key);
        values.put(COLUMN_VALUE, value);
        db.insertWithOnConflict(TABLE_METADATA, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static String getMetadata(SQLiteDatabase db, String key, String defaultValue) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_METADATA, new String[]{COLUMN_VALUE},
                    String.format("%s = ?", COLUMN_KEY), new String[]{key}, null, null, null);
            if (cursor.getCount() > 0 && cursor.moveToNext())
                return cursor.getString(cursor.getColumnIndex(COLUMN_VALUE));
            return defaultValue;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }
}
