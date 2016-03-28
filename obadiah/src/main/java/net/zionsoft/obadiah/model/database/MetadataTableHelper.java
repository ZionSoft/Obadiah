/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2016 ZionSoft
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

import net.zionsoft.obadiah.utils.TextFormatter;

public class MetadataTableHelper {
    private static final String TABLE_METADATA = "TABLE_METADATA";
    private static final String COLUMN_KEY = "COLUMN_KEY";
    private static final String COLUMN_VALUE = "COLUMN_VALUE";

    public static final String KEY_LAST_READING_TIMESTAMP = "KEY_LAST_READING_TIMESTAMP";
    public static final String KEY_CONTINUOUS_READING_DAYS = "KEY_CONTINUOUS_READING_DAYS";

    static void createTable(SQLiteDatabase db) {
        db.execSQL(TextFormatter.format("CREATE TABLE %s (%s TEXT PRIMARY KEY, %s TEXT NOT NULL);",
                TABLE_METADATA, COLUMN_KEY, COLUMN_VALUE));
    }

    public static void saveMetadata(SQLiteDatabase db, String key, String value) {
        final ContentValues values = new ContentValues(2);
        values.put(COLUMN_KEY, key);
        values.put(COLUMN_VALUE, value);
        db.insertWithOnConflict(TABLE_METADATA, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @NonNull
    public static String getMetadata(SQLiteDatabase db, String key, String defaultValue) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_METADATA, new String[]{COLUMN_VALUE},
                    TextFormatter.format("%s = ?", COLUMN_KEY), new String[]{key}, null, null, null);
            if (cursor.getCount() > 0 && cursor.moveToNext()) {
                return cursor.getString(cursor.getColumnIndex(COLUMN_VALUE));
            }
            return defaultValue;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }
}
