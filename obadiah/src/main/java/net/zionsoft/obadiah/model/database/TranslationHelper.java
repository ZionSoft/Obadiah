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
import android.support.annotation.Nullable;

import net.zionsoft.obadiah.model.domain.Bible;
import net.zionsoft.obadiah.model.domain.TranslationInfo;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.network.BackendTranslationInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TranslationHelper {
    public static List<String> getDownloadedTranslationShortNames(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.query(true, DatabaseHelper.TABLE_BOOK_NAMES,
                    new String[]{DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME},
                    null, null, null, null, null, null);
            final int translationShortName = cursor.getColumnIndex(
                    DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME);
            final List<String> translations = new ArrayList<String>(cursor.getCount());
            while (cursor.moveToNext())
                translations.add(cursor.getString(translationShortName));
            return translations;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public static List<String> getBookNames(SQLiteDatabase db, String translationShortName) {
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.TABLE_BOOK_NAMES,
                    new String[]{DatabaseHelper.COLUMN_BOOK_NAME},
                    String.format("%s = ?", DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME),
                    new String[]{translationShortName}, null, null,
                    String.format("%s ASC", DatabaseHelper.COLUMN_BOOK_INDEX));
            final int bookName = cursor.getColumnIndex(DatabaseHelper.COLUMN_BOOK_NAME);
            final List<String> bookNames = new ArrayList<String>(Bible.getBookCount());
            while (cursor.moveToNext())
                bookNames.add(cursor.getString(bookName));
            return bookNames;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    @Nullable
    public static Verse getVerse(SQLiteDatabase db, String translationShortName,
                                 String bookName, int book, int chapter, int verse) {
        Cursor cursor = null;
        try {
            cursor = db.query(translationShortName,
                    new String[]{DatabaseHelper.COLUMN_TEXT},
                    String.format("%s = ? AND %s = ? AND %s = ?", DatabaseHelper.COLUMN_BOOK_INDEX,
                            DatabaseHelper.COLUMN_CHAPTER_INDEX, DatabaseHelper.COLUMN_VERSE_INDEX),
                    new String[]{Integer.toString(book), Integer.toString(chapter), Integer.toString(verse)},
                    null, null, null);
            if (cursor.moveToFirst()) {
                return new Verse(book, chapter, verse, bookName, cursor.getString(0));
            } else {
                return null;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public static List<Verse> getVerses(SQLiteDatabase db, String translationShortName,
                                        String bookName, int book, int chapter) {
        Cursor cursor = null;
        try {
            cursor = db.query(translationShortName,
                    new String[]{DatabaseHelper.COLUMN_TEXT},
                    String.format("%s = ? AND %s = ?",
                            DatabaseHelper.COLUMN_BOOK_INDEX, DatabaseHelper.COLUMN_CHAPTER_INDEX),
                    new String[]{Integer.toString(book), Integer.toString(chapter)},
                    null, null, String.format("%s ASC", DatabaseHelper.COLUMN_VERSE_INDEX)
            );
            final int verse = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT);
            final List<Verse> verses = new ArrayList<Verse>(cursor.getCount());
            int verseIndex = 0;
            while (cursor.moveToNext())
                verses.add(new Verse(book, chapter, verseIndex++, bookName, cursor.getString(verse)));
            return verses;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public static List<Verse> searchVerses(SQLiteDatabase db, String translationShortName,
                                           List<String> bookNames, String keyword) {
        Cursor cursor = null;
        try {
            cursor = db.query(translationShortName,
                    new String[]{DatabaseHelper.COLUMN_BOOK_INDEX, DatabaseHelper.COLUMN_CHAPTER_INDEX,
                            DatabaseHelper.COLUMN_VERSE_INDEX, DatabaseHelper.COLUMN_TEXT},
                    String.format("%s LIKE ?", DatabaseHelper.COLUMN_TEXT),
                    new String[]{String.format("%%%s%%", keyword.trim().replaceAll("\\s+", "%"))},
                    null, null, String.format(" %s ASC, %s ASC, %s ASC", DatabaseHelper.COLUMN_BOOK_INDEX,
                            DatabaseHelper.COLUMN_CHAPTER_INDEX, DatabaseHelper.COLUMN_VERSE_INDEX));
            final int count = cursor.getCount();
            if (count == 0)
                return Collections.emptyList();

            final int bookIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_BOOK_INDEX);
            final int chapterIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CHAPTER_INDEX);
            final int verseIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_VERSE_INDEX);
            final int verseText = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT);
            final List<Verse> verses = new ArrayList<Verse>(count);
            while (cursor.moveToNext()) {
                final int book = cursor.getInt(bookIndex);
                verses.add(new Verse(book, cursor.getInt(chapterIndex), cursor.getInt(verseIndex),
                        bookNames.get(book), cursor.getString(verseText)));
            }
            return verses;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public static void createTranslationTable(SQLiteDatabase db, String translationShortName) {
        db.execSQL(String.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL);",
                translationShortName, DatabaseHelper.COLUMN_BOOK_INDEX, DatabaseHelper.COLUMN_CHAPTER_INDEX,
                DatabaseHelper.COLUMN_VERSE_INDEX, DatabaseHelper.COLUMN_TEXT));
        db.execSQL(String.format("CREATE INDEX INDEX_%s ON %s (%s, %s, %s);",
                translationShortName, translationShortName, DatabaseHelper.COLUMN_BOOK_INDEX,
                DatabaseHelper.COLUMN_CHAPTER_INDEX, DatabaseHelper.COLUMN_VERSE_INDEX));
    }

    public static void saveBookNames(SQLiteDatabase db, BackendTranslationInfo translation) {
        final ContentValues bookNamesValues = new ContentValues(3);
        bookNamesValues.put(DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME, translation.shortName);
        final List<String> books = translation.books;
        final int count = books.size();
        for (int i = 0; i < count; ++i) {
            bookNamesValues.put(DatabaseHelper.COLUMN_BOOK_INDEX, i);
            bookNamesValues.put(DatabaseHelper.COLUMN_BOOK_NAME, books.get(i));
            db.insert(DatabaseHelper.TABLE_BOOK_NAMES, null, bookNamesValues);
        }
    }

    public static void saveVerses(SQLiteDatabase db, String translation, int book, int chapter, List<String> verses) {
        final ContentValues versesValues = new ContentValues(4);
        versesValues.put(DatabaseHelper.COLUMN_BOOK_INDEX, book);
        versesValues.put(DatabaseHelper.COLUMN_CHAPTER_INDEX, chapter);
        final int versesCount = verses.size();
        for (int i = 0; i < versesCount; ++i) {
            versesValues.put(DatabaseHelper.COLUMN_VERSE_INDEX, i);
            versesValues.put(DatabaseHelper.COLUMN_TEXT, verses.get(i));
            db.insert(translation, null, versesValues);
        }
    }

    public static void removeTranslation(SQLiteDatabase db, String translationShortName) {
        db.execSQL(String.format("DROP TABLE IF EXISTS %s", translationShortName));

        db.delete(DatabaseHelper.TABLE_BOOK_NAMES,
                String.format("%s = ?", DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME),
                new String[]{translationShortName});
    }

    public static void saveTranslations(SQLiteDatabase db, List<TranslationInfo> translations) {
        final ContentValues values = new ContentValues(5);
        final int size = translations.size();
        for (int i = 0; i < size; ++i) {
            final TranslationInfo translation = translations.get(i);
            values.put(DatabaseHelper.COLUMN_TRANSLATION_NAME, translation.name);
            values.put(DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME, translation.shortName);
            values.put(DatabaseHelper.COLUMN_TRANSLATION_LANGUAGE, translation.language);
            values.put(DatabaseHelper.COLUMN_TRANSLATION_BLOB_KEY, translation.blobKey);
            values.put(DatabaseHelper.COLUMN_TRANSLATION_SIZE, translation.size);
            db.insertWithOnConflict(DatabaseHelper.TABLE_TRANSLATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public static List<TranslationInfo> getTranslations(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.TABLE_TRANSLATIONS,
                    new String[]{DatabaseHelper.COLUMN_TRANSLATION_NAME, DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME,
                            DatabaseHelper.COLUMN_TRANSLATION_LANGUAGE, DatabaseHelper.COLUMN_TRANSLATION_BLOB_KEY,
                            DatabaseHelper.COLUMN_TRANSLATION_SIZE}, null, null, null, null, null, null);
            final int name = cursor.getColumnIndex(DatabaseHelper.COLUMN_TRANSLATION_NAME);
            final int shortName = cursor.getColumnIndex(DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME);
            final int language = cursor.getColumnIndex(DatabaseHelper.COLUMN_TRANSLATION_LANGUAGE);
            final int blobKey = cursor.getColumnIndex(DatabaseHelper.COLUMN_TRANSLATION_BLOB_KEY);
            final int size = cursor.getColumnIndex(DatabaseHelper.COLUMN_TRANSLATION_SIZE);
            final List<TranslationInfo> translations = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                translations.add(new TranslationInfo(cursor.getString(name), cursor.getString(shortName),
                        cursor.getString(language), cursor.getString(blobKey), cursor.getInt(size)));
            }
            return translations;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
