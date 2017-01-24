/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2017 ZionSoft
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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.utils.TextFormatter;

import java.util.ArrayList;
import java.util.List;

public class BookmarkTableHelper {
    private static final String TABLE_BOOKMARK = "TABLE_BOOKMARK";

    static void createTable(SQLiteDatabase db) {
        db.execSQL(TextFormatter.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, PRIMARY KEY (%s, %s, %s));",
                TABLE_BOOKMARK, VerseIndex.ColumnNames.BOOK_INDEX,
                VerseIndex.ColumnNames.CHAPTER_INDEX, VerseIndex.ColumnNames.VERSE_INDEX,
                Bookmark.ColumnNames.TIMESTAMP, VerseIndex.ColumnNames.BOOK_INDEX,
                VerseIndex.ColumnNames.CHAPTER_INDEX, VerseIndex.ColumnNames.VERSE_INDEX));
    }

    public static void saveBookmark(SQLiteDatabase db, Bookmark bookmark) {
        db.insertWithOnConflict(TABLE_BOOKMARK, null,
                bookmark.toContentValues(null), SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void removeBookmark(SQLiteDatabase db, VerseIndex verseIndex) {
        db.delete(TABLE_BOOKMARK, TextFormatter.format("%s = ? AND %s = ? AND %s = ?",
                VerseIndex.ColumnNames.BOOK_INDEX, VerseIndex.ColumnNames.CHAPTER_INDEX,
                VerseIndex.ColumnNames.VERSE_INDEX),
                new String[]{Integer.toString(verseIndex.book()), Integer.toString(verseIndex.chapter()),
                        Integer.toString(verseIndex.verse())});
    }

    @Nullable
    public static Bookmark getBookmark(SQLiteDatabase db, VerseIndex verseIndex) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_BOOKMARK, null, TextFormatter.format("%s = ? AND %s = ? AND %s = ?",
                    VerseIndex.ColumnNames.BOOK_INDEX, VerseIndex.ColumnNames.CHAPTER_INDEX,
                    VerseIndex.ColumnNames.VERSE_INDEX),
                    new String[]{Integer.toString(verseIndex.book()), Integer.toString(verseIndex.chapter()),
                            Integer.toString(verseIndex.verse())},
                    null, null, null);
            if (cursor.moveToFirst()) {
                return Bookmark.create(cursor);
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @NonNull
    public static List<Bookmark> getBookmarks(SQLiteDatabase db, int book, int chapter) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_BOOKMARK, null, TextFormatter.format("%s = ? AND %s = ?",
                    VerseIndex.ColumnNames.BOOK_INDEX, VerseIndex.ColumnNames.CHAPTER_INDEX),
                    new String[]{Integer.toString(book), Integer.toString(chapter)},
                    null, null, TextFormatter.format("%s ASC", VerseIndex.ColumnNames.VERSE_INDEX));
            final List<Bookmark> bookmarks = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                bookmarks.add(Bookmark.create(cursor));
            }
            return bookmarks;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @NonNull
    public static List<Bookmark> getBookmarks(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_BOOKMARK, null, null, null, null, null,
                    TextFormatter.format("%s DESC", Bookmark.ColumnNames.TIMESTAMP));
            final List<Bookmark> bookmarks = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                bookmarks.add(Bookmark.create(cursor));
            }
            return bookmarks;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
