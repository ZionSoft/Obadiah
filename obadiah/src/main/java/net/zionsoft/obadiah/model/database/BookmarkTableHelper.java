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
import android.database.sqlite.SQLiteDatabase;

import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.Verse;

public class BookmarkTableHelper {
    private static final String TABLE_BOOKMARK = "TABLE_BOOKMARK";
    private static final String COLUMN_TIMESTAMP = "COLUMN_TIMESTAMP";
    private static final String COLUMN_BOOK_INDEX = "COLUMN_BOOK_INDEX";
    private static final String COLUMN_CHAPTER_INDEX = "COLUMN_CHAPTER_INDEX";
    private static final String COLUMN_VERSE_INDEX = "COLUMN_VERSE_INDEX";

    static void createTable(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, PRIMARY KEY (%s, %s, %s));",
                TABLE_BOOKMARK, COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX,
                COLUMN_TIMESTAMP, COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX));
    }

    public static void saveBookmark(SQLiteDatabase db, Bookmark bookmark) {
        final ContentValues bookmarkValues = new ContentValues(4);
        bookmarkValues.put(COLUMN_BOOK_INDEX, bookmark.verseIndex.book);
        bookmarkValues.put(COLUMN_CHAPTER_INDEX, bookmark.verseIndex.chapter);
        bookmarkValues.put(COLUMN_VERSE_INDEX, bookmark.verseIndex.verse);
        bookmarkValues.put(COLUMN_TIMESTAMP, bookmark.timestamp);
        db.insertWithOnConflict(TABLE_BOOKMARK, null, bookmarkValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void removeBookmark(SQLiteDatabase db, Verse.Index verseIndex) {
        db.delete(TABLE_BOOKMARK, String.format("%s = ? AND %s = ? AND %s = ?",
                        COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX),
                new String[]{Integer.toString(verseIndex.book), Integer.toString(verseIndex.chapter),
                        Integer.toString(verseIndex.verse)});
    }
}
