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

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import net.zionsoft.obadiah.model.domain.Note;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.utils.TextFormatter;

import java.util.ArrayList;
import java.util.List;

public class NoteTableHelper {
    private static final String TABLE_NOTE = "TABLE_NOTE";

    static void createTable(SQLiteDatabase db) {
        db.execSQL(TextFormatter.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL, %s INTEGER NOT NULL, PRIMARY KEY (%s, %s, %s));",
                TABLE_NOTE, VerseIndex.ColumnNames.BOOK_INDEX, VerseIndex.ColumnNames.CHAPTER_INDEX,
                VerseIndex.ColumnNames.VERSE_INDEX, Note.ColumnNames.NOTE, Note.ColumnNames.TIMESTAMP,
                VerseIndex.ColumnNames.BOOK_INDEX, VerseIndex.ColumnNames.CHAPTER_INDEX,
                VerseIndex.ColumnNames.VERSE_INDEX));
    }

    public static void saveNote(SQLiteDatabase db, Note note) {
        db.insertWithOnConflict(TABLE_NOTE, null, note.toContentValues(null), SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void removeNote(SQLiteDatabase db, VerseIndex verseIndex) {
        db.delete(TABLE_NOTE, TextFormatter.format("%s = ? AND %s = ? AND %s = ?",
                VerseIndex.ColumnNames.BOOK_INDEX, VerseIndex.ColumnNames.CHAPTER_INDEX,
                VerseIndex.ColumnNames.VERSE_INDEX),
                new String[]{Integer.toString(verseIndex.book()), Integer.toString(verseIndex.chapter()),
                        Integer.toString(verseIndex.verse())});
    }

    public static boolean hasNote(SQLiteDatabase db, VerseIndex verseIndex) {
        return DatabaseUtils.queryNumEntries(db, TABLE_NOTE, TextFormatter.format("%s = ? AND %s = ? AND %s = ?",
                VerseIndex.ColumnNames.BOOK_INDEX, VerseIndex.ColumnNames.CHAPTER_INDEX,
                VerseIndex.ColumnNames.VERSE_INDEX), new String[]{Integer.toString(verseIndex.book()),
                Integer.toString(verseIndex.chapter()), Integer.toString(verseIndex.verse())}) > 0L;
    }

    @NonNull
    public static List<Note> getNotes(SQLiteDatabase db, int book, int chapter) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NOTE, null, TextFormatter.format("%s = ? AND %s = ?",
                    VerseIndex.ColumnNames.BOOK_INDEX, VerseIndex.ColumnNames.CHAPTER_INDEX),
                    new String[]{Integer.toString(book), Integer.toString(chapter)},
                    null, null, TextFormatter.format("%s ASC", VerseIndex.ColumnNames.VERSE_INDEX));
            final List<Note> notes = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                notes.add(Note.create(cursor));
            }
            return notes;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @NonNull
    public static List<Note> getNotes(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NOTE, null, null, null, null, null,
                    TextFormatter.format("%s DESC", Note.ColumnNames.TIMESTAMP));
            final List<Note> notes = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                notes.add(Note.create(cursor));
            }
            return notes;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
