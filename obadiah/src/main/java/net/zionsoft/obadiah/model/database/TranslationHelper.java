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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.model.domain.VerseSearchResult;
import net.zionsoft.obadiah.utils.TextFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TranslationHelper {
    private static final String COLUMN_BOOK_INDEX = "COLUMN_BOOK_INDEX";
    private static final String COLUMN_CHAPTER_INDEX = "COLUMN_CHAPTER_INDEX";
    private static final String COLUMN_VERSE_INDEX = "COLUMN_VERSE_INDEX";
    private static final String COLUMN_TEXT = "COLUMN_TEXT";

    public static void createTranslationTable(SQLiteDatabase db, String translationShortName) {
        db.execSQL(TextFormatter.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL);",
                translationShortName, COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX,
                COLUMN_VERSE_INDEX, COLUMN_TEXT));
        db.execSQL(TextFormatter.format("CREATE INDEX INDEX_%s ON %s (%s, %s, %s);",
                translationShortName, translationShortName, COLUMN_BOOK_INDEX,
                COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX));
    }

    @Nullable
    public static Verse getVerse(SQLiteDatabase db, String translationShortName,
                                 String bookName, int book, int chapter, int verse) {
        Cursor cursor = null;
        try {
            cursor = db.query(translationShortName, new String[]{COLUMN_TEXT},
                    TextFormatter.format("%s = ? AND %s = ? AND %s = ?", COLUMN_BOOK_INDEX,
                            COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX),
                    new String[]{Integer.toString(book), Integer.toString(chapter), Integer.toString(verse)},
                    null, null, null);
            if (cursor.moveToFirst()) {
                return new Verse(VerseIndex.create(book, chapter, verse),
                        new Verse.Text(translationShortName, bookName, cursor.getString(0)), null);
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
                    TextFormatter.format("%s = ? AND %s = ?", COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX),
                    new String[]{Integer.toString(book), Integer.toString(chapter)},
                    null, null, TextFormatter.format("%s ASC", COLUMN_VERSE_INDEX)
            );
            final int verse = cursor.getColumnIndex(COLUMN_TEXT);
            final int verseCount = cursor.getCount();
            final List<Verse> verses = new ArrayList<>(verseCount);
            int verseIndex = 0;
            boolean hasNonEmptyVerseInTheBeginning = false;
            while (cursor.moveToNext()) {
                // ignores empty verses at the beginning
                final String text = cursor.getString(verse);
                if (hasNonEmptyVerseInTheBeginning || !TextUtils.isEmpty(text)) {
                    verses.add(new Verse(VerseIndex.create(book, chapter, verseIndex++),
                            new Verse.Text(translationShortName, bookName, text), null));
                    hasNonEmptyVerseInTheBeginning = true;
                }
            }

            // removes trailing verses at the end
            for (int i = verseCount - 1; i >= 0; --i) {
                if (TextUtils.isEmpty(verses.get(i).text.text)) {
                    verses.remove(i);
                } else {
                    break;
                }
            }

            return verses;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @NonNull
    public static List<String> getVerseTexts(SQLiteDatabase db, String translationShortName, int book, int chapter) {
        Cursor cursor = null;
        try {
            cursor = db.query(translationShortName, new String[]{COLUMN_TEXT},
                    TextFormatter.format("%s = ? AND %s = ?", COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX),
                    new String[]{Integer.toString(book), Integer.toString(chapter)},
                    null, null, TextFormatter.format("%s ASC", COLUMN_VERSE_INDEX)
            );
            final int verse = cursor.getColumnIndex(COLUMN_TEXT);
            final int verseCount = cursor.getCount();
            final List<String> verseTexts = new ArrayList<>(verseCount);
            boolean hasNonEmptyVerseInTheBeginning = false;
            while (cursor.moveToNext()) {
                // ignores empty verses at the beginning
                final String text = cursor.getString(verse);
                if (hasNonEmptyVerseInTheBeginning || !TextUtils.isEmpty(text)) {
                    verseTexts.add(text);
                    hasNonEmptyVerseInTheBeginning = true;
                }
            }

            // removes trailing verses at the end
            for (int i = verseCount - 1; i >= 0; --i) {
                if (TextUtils.isEmpty(verseTexts.get(i))) {
                    verseTexts.remove(i);
                } else {
                    break;
                }
            }

            return verseTexts;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @NonNull
    public static List<VerseSearchResult> searchVerses(SQLiteDatabase db, String translationShortName,
                                                       List<String> bookNames, String query, int offset, int limit) {
        Cursor cursor = null;
        try {
            final String[] keywords = query.trim().replaceAll("\\s+", " ").split(" ");
            if (keywords.length == 0) {
                return Collections.emptyList();
            }

            final String singleSelection = TextFormatter.format("%s LIKE ?", COLUMN_TEXT);
            final StringBuilder selection = new StringBuilder();
            final String[] selectionArgs = new String[keywords.length];
            for (int i = 0; i < keywords.length; ++i) {
                if (i > 0) {
                    selection.append(" AND ");
                }
                selection.append(singleSelection);

                selectionArgs[i] = TextFormatter.format("%%%s%%", keywords[i]);
            }

            cursor = db.query(translationShortName,
                    new String[]{COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX, COLUMN_TEXT},
                    selection.toString(), selectionArgs, null, null,
                    TextFormatter.format(" %s ASC, %s ASC, %s ASC",
                            COLUMN_BOOK_INDEX, COLUMN_CHAPTER_INDEX, COLUMN_VERSE_INDEX),
                    TextFormatter.format("%d, %d", offset, limit));
            final int count = cursor.getCount();
            if (count == 0) {
                return Collections.emptyList();
            }

            final int bookIndex = cursor.getColumnIndex(COLUMN_BOOK_INDEX);
            final int chapterIndex = cursor.getColumnIndex(COLUMN_CHAPTER_INDEX);
            final int verseIndex = cursor.getColumnIndex(COLUMN_VERSE_INDEX);
            final int verseText = cursor.getColumnIndex(COLUMN_TEXT);
            final List<VerseSearchResult> verses = new ArrayList<>(count);
            while (cursor.moveToNext()) {
                final int book = cursor.getInt(bookIndex);
                final VerseIndex index = VerseIndex.create(
                        book, cursor.getInt(chapterIndex), cursor.getInt(verseIndex));
                verses.add(new VerseSearchResult(index, bookNames.get(book), cursor.getString(verseText)));
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
        db.execSQL(TextFormatter.format("DROP TABLE IF EXISTS %s", translationShortName));
    }
}
