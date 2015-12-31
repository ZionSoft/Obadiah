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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.zionsoft.obadiah.model.domain.Verse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TranslationHelper {
    private static final String COLUMN_BOOK_INDEX = "COLUMN_BOOK_INDEX";
    private static final String COLUMN_CHAPTER_INDEX = "COLUMN_CHAPTER_INDEX";
    private static final String COLUMN_VERSE_INDEX = "COLUMN_VERSE_INDEX";
    private static final String COLUMN_TEXT = "COLUMN_TEXT";

    public static void createTranslationTable(SQLiteDatabase db, String translationShortName) {
        db.execSQL(String.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL);",
                translationShortName, COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX,
                COLUMN_VERSE_INDEX, COLUMN_TEXT));
        db.execSQL(String.format("CREATE INDEX INDEX_%s ON %s (%s, %s, %s);",
                translationShortName, translationShortName, COLUMN_BOOK_INDEX,
                COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX));
    }

    @Nullable
    public static Verse getVerse(SQLiteDatabase db, String translationShortName,
                                 String bookName, int book, int chapter, int verse) {
        Cursor cursor = null;
        try {
            cursor = db.query(translationShortName, new String[]{COLUMN_TEXT},
                    String.format("%s = ? AND %s = ? AND %s = ?", COLUMN_BOOK_INDEX,
                            COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX),
                    new String[]{Integer.toString(book), Integer.toString(chapter), Integer.toString(verse)},
                    null, null, null);
            if (cursor.moveToFirst()) {
                return new Verse(new Verse.Index(book, chapter, verse), bookName, cursor.getString(0));
            } else {
                return null;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    @NonNull
    public static List<Verse> getVerses(SQLiteDatabase db, String translationShortName,
                                        String bookName, int book, int chapter) {
        Cursor cursor = null;
        try {
            cursor = db.query(translationShortName, new String[]{COLUMN_TEXT},
                    String.format("%s = ? AND %s = ?", COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX),
                    new String[]{Integer.toString(book), Integer.toString(chapter)},
                    null, null, String.format("%s ASC", COLUMN_VERSE_INDEX)
            );
            final int verse = cursor.getColumnIndex(COLUMN_TEXT);
            final List<Verse> verses = new ArrayList<>(cursor.getCount());
            int verseIndex = 0;
            while (cursor.moveToNext()) {
                verses.add(new Verse(new Verse.Index(book, chapter, verseIndex++), bookName, cursor.getString(verse)));
            }
            return verses;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @NonNull
    public static List<Verse> searchVerses(SQLiteDatabase db, String translationShortName,
                                           List<String> bookNames, String keyword) {
        Cursor cursor = null;
        try {
            cursor = db.query(translationShortName,
                    new String[]{COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX, COLUMN_TEXT},
                    String.format("%s LIKE ?", COLUMN_TEXT),
                    new String[]{String.format("%%%s%%", keyword.trim().replaceAll("\\s+", "%"))},
                    null, null, String.format(" %s ASC, %s ASC, %s ASC", COLUMN_BOOK_INDEX,
                            COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX));
            final int count = cursor.getCount();
            if (count == 0) {
                return Collections.emptyList();
            }

            final int bookIndex = cursor.getColumnIndex(COLUMN_BOOK_INDEX);
            final int chapterIndex = cursor.getColumnIndex(COLUMN_CHAPTER_INDEX);
            final int verseIndex = cursor.getColumnIndex(COLUMN_VERSE_INDEX);
            final int verseText = cursor.getColumnIndex(COLUMN_TEXT);
            final List<Verse> verses = new ArrayList<>(count);
            while (cursor.moveToNext()) {
                final int book = cursor.getInt(bookIndex);
                final Verse.Index index = new Verse.Index(
                        book, cursor.getInt(chapterIndex), cursor.getInt(verseIndex));
                verses.add(new Verse(index, bookNames.get(book), cursor.getString(verseText)));
            }
            return verses;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void saveVerses(SQLiteDatabase db, String translation, int book, int chapter, List<String> verses) {
        final ContentValues versesValues = new ContentValues(4);
        versesValues.put(COLUMN_BOOK_INDEX, book);
        versesValues.put(COLUMN_CHAPTER_INDEX, chapter);
        final int versesCount = verses.size();
        for (int i = 0; i < versesCount; ++i) {
            versesValues.put(COLUMN_VERSE_INDEX, i);
            versesValues.put(COLUMN_TEXT, verses.get(i));
            db.insert(translation, null, versesValues);
        }
    }

    public static void removeTranslation(SQLiteDatabase db, String translationShortName) {
        db.execSQL(String.format("DROP TABLE IF EXISTS %s", translationShortName));
    }
}
