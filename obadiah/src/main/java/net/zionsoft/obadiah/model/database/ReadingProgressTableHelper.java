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

import net.zionsoft.obadiah.model.domain.ReadingProgress;
import net.zionsoft.obadiah.utils.TextFormatter;

import java.util.ArrayList;
import java.util.List;

public class ReadingProgressTableHelper {
    private static final String TABLE_READING_PROGRESS = "TABLE_READING_PROGRESS";
    private static final String COLUMN_BOOK_INDEX = "COLUMN_BOOK_INDEX";
    private static final String COLUMN_CHAPTER_INDEX = "COLUMN_CHAPTER_INDEX";
    private static final String COLUMN_LAST_READING_TIMESTAMP = "COLUMN_LAST_READING_TIMESTAMP";

    static void createTable(SQLiteDatabase db) {
        db.execSQL(TextFormatter.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, PRIMARY KEY (%s, %s));",
                TABLE_READING_PROGRESS, COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_LAST_READING_TIMESTAMP,
                COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX));
    }

    @NonNull
    public static List<ReadingProgress.ReadChapter> getChaptersReadPerBook(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_READING_PROGRESS, new String[]{COLUMN_BOOK_INDEX,
                            COLUMN_CHAPTER_INDEX, COLUMN_LAST_READING_TIMESTAMP},
                    null, null, null, null, TextFormatter.format("%s, %s ASC", COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX));
            final int bookIndex = cursor.getColumnIndex(COLUMN_BOOK_INDEX);
            final int chapterIndex = cursor.getColumnIndex(COLUMN_CHAPTER_INDEX);
            final int lastReadingTimestamp = cursor.getColumnIndex(COLUMN_LAST_READING_TIMESTAMP);

            final List<ReadingProgress.ReadChapter> readChapters = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                readChapters.add(new ReadingProgress.ReadChapter(cursor.getInt(bookIndex),
                        cursor.getInt(chapterIndex), cursor.getLong(lastReadingTimestamp)));
            }
            return readChapters;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void saveChapterReading(SQLiteDatabase db, int book, int chapter, long timestamp) {
        final ContentValues values = new ContentValues(3);
        values.put(COLUMN_BOOK_INDEX, book);
        values.put(COLUMN_CHAPTER_INDEX, chapter);
        values.put(COLUMN_LAST_READING_TIMESTAMP, timestamp);
        db.insertWithOnConflict(TABLE_READING_PROGRESS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static long getChapterReadTimestamp(SQLiteDatabase db, int book, int chapter) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_READING_PROGRESS, new String[]{COLUMN_LAST_READING_TIMESTAMP},
                    TextFormatter.format("%s = ? AND %s = ?", COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX),
                    new String[]{Integer.toString(book), Integer.toString(chapter)}, null, null, null);
            return cursor.moveToFirst() ? cursor.getLong(0) : 0L;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
