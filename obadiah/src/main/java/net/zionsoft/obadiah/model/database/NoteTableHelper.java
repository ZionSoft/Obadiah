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
    private static final String COLUMN_TIMESTAMP = "COLUMN_TIMESTAMP";
    private static final String COLUMN_NOTE = "COLUMN_NOTE";
    private static final String COLUMN_BOOK_INDEX = "COLUMN_BOOK_INDEX";
    private static final String COLUMN_CHAPTER_INDEX = "COLUMN_CHAPTER_INDEX";
    private static final String COLUMN_VERSE_INDEX = "COLUMN_VERSE_INDEX";

    static void createTable(SQLiteDatabase db) {
        db.execSQL(TextFormatter.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL, %s INTEGER NOT NULL, PRIMARY KEY (%s, %s, %s));",
                TABLE_NOTE, COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX, COLUMN_NOTE,
                COLUMN_TIMESTAMP, COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX));
    }

    public static void saveNote(SQLiteDatabase db, Note note) {
        final ContentValues bookmarkValues = new ContentValues(5);
        bookmarkValues.put(COLUMN_BOOK_INDEX, note.verseIndex.book());
        bookmarkValues.put(COLUMN_CHAPTER_INDEX, note.verseIndex.chapter());
        bookmarkValues.put(COLUMN_VERSE_INDEX, note.verseIndex.verse());
        bookmarkValues.put(COLUMN_NOTE, note.note);
        bookmarkValues.put(COLUMN_TIMESTAMP, note.timestamp);
        db.insertWithOnConflict(TABLE_NOTE, null, bookmarkValues, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void removeNote(SQLiteDatabase db, VerseIndex verseIndex) {
        db.delete(TABLE_NOTE, TextFormatter.format("%s = ? AND %s = ? AND %s = ?",
                        COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX),
                new String[]{Integer.toString(verseIndex.book()), Integer.toString(verseIndex.chapter()),
                        Integer.toString(verseIndex.verse())});
    }

    public static boolean hasNote(SQLiteDatabase db, VerseIndex verseIndex) {
        return DatabaseUtils.queryNumEntries(db, TABLE_NOTE, TextFormatter.format("%s = ? AND %s = ? AND %s = ?",
                        COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX),
                new String[]{Integer.toString(verseIndex.book()), Integer.toString(verseIndex.chapter()),
                        Integer.toString(verseIndex.verse())}) > 0L;
    }

    @NonNull
    public static List<Note> getNotes(SQLiteDatabase db, int book, int chapter) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NOTE, new String[]{COLUMN_TIMESTAMP, COLUMN_NOTE, COLUMN_VERSE_INDEX},
                    TextFormatter.format("%s = ? AND %s = ?", COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX),
                    new String[]{Integer.toString(book), Integer.toString(chapter)},
                    null, null, TextFormatter.format("%s ASC", COLUMN_VERSE_INDEX));
            final int timestamp = cursor.getColumnIndex(COLUMN_TIMESTAMP);
            final int note = cursor.getColumnIndex(COLUMN_NOTE);
            final int verseIndex = cursor.getColumnIndex(COLUMN_VERSE_INDEX);
            final int noteCount = cursor.getCount();
            final List<Note> notes = new ArrayList<>(noteCount);
            while (cursor.moveToNext()) {
                notes.add(new Note(VerseIndex.create(book, chapter, cursor.getInt(verseIndex)),
                        cursor.getString(note), cursor.getLong(timestamp)));
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
            cursor = db.query(TABLE_NOTE, new String[]{COLUMN_TIMESTAMP, COLUMN_NOTE, COLUMN_BOOK_INDEX,
                            COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX}, null, null, null, null,
                    TextFormatter.format("%s DESC", COLUMN_TIMESTAMP));
            final int timestamp = cursor.getColumnIndex(COLUMN_TIMESTAMP);
            final int note = cursor.getColumnIndex(COLUMN_NOTE);
            final int bookIndex = cursor.getColumnIndex(COLUMN_BOOK_INDEX);
            final int chapterIndex = cursor.getColumnIndex(COLUMN_CHAPTER_INDEX);
            final int verseIndex = cursor.getColumnIndex(COLUMN_VERSE_INDEX);
            final int notesCount = cursor.getCount();
            final List<Note> notes = new ArrayList<>(notesCount);
            while (cursor.moveToNext()) {
                notes.add(new Note(VerseIndex.create(cursor.getInt(bookIndex),
                        cursor.getInt(chapterIndex), cursor.getInt(verseIndex)),
                        cursor.getString(note), cursor.getLong(timestamp)));
            }
            return notes;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
