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

package net.zionsoft.obadiah.model.translations;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.Verse;
import net.zionsoft.obadiah.model.database.DatabaseHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TranslationHelper {
    public static List<TranslationInfo> sortByLocale(List<TranslationInfo> translations) {
        Collections.sort(translations, new Comparator<TranslationInfo>() {
            @Override
            public int compare(TranslationInfo translation1, TranslationInfo translation2) {
                // first compares with user's default locale
                final Locale userLocale = Locale.getDefault();
                final String userLanguage = userLocale.getLanguage().toLowerCase();
                final String userCountry = userLocale.getCountry().toLowerCase();
                final String[] fields1 = translation1.language.split("_");
                final String[] fields2 = translation2.language.split("_");
                final int score1 = compareLocale(fields1[0], fields1[1],
                        userLanguage, userCountry);
                final int score2 = compareLocale(fields2[0], fields2[1],
                        userLanguage, userCountry);
                int r = score2 - score1;
                if (r != 0)
                    return r;

                // then sorts by language & name
                r = translation1.language.compareTo(translation2.language);
                return r == 0 ? translation1.name.compareTo(translation2.name) : r;
            }
        });
        return translations;
    }

    private static int compareLocale(String language, String country, String targetLanguage, String targetCountry) {
        if (language.equals(targetLanguage))
            return (country.equals(targetCountry)) ? 2 : 1;
        return 0;
    }

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

    public static void saveBookNames(SQLiteDatabase db, JSONObject booksInfoObject) throws Exception {
        final String translationShortName = booksInfoObject.getString("shortName");
        final ContentValues bookNamesValues = new ContentValues(3);
        bookNamesValues.put(DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME, translationShortName);
        final JSONArray booksArray = booksInfoObject.getJSONArray("books");
        for (int i = 0; i < Bible.getBookCount(); ++i) {
            bookNamesValues.put(DatabaseHelper.COLUMN_BOOK_INDEX, i);

            final String bookName = booksArray.getString(i);
            if (TextUtils.isEmpty(bookName))
                throw new Exception("Illegal books.json file: " + translationShortName);
            bookNamesValues.put(DatabaseHelper.COLUMN_BOOK_NAME, bookName);

            db.insert(DatabaseHelper.TABLE_BOOK_NAMES, null, bookNamesValues);
        }
    }

    public static void saveVerses(SQLiteDatabase db, String translationShortName,
                                  int bookIndex, int chapterIndex, JSONObject versesObject) throws Exception {
        final ContentValues versesValues = new ContentValues(4);
        versesValues.put(DatabaseHelper.COLUMN_BOOK_INDEX, bookIndex);
        versesValues.put(DatabaseHelper.COLUMN_CHAPTER_INDEX, chapterIndex);
        final JSONArray paragraphArray = versesObject.getJSONArray("verses");
        final int paragraphCount = paragraphArray.length();
        boolean hasNonEmptyVerse = false;
        for (int verseIndex = 0; verseIndex < paragraphCount; ++verseIndex) {
            versesValues.put(DatabaseHelper.COLUMN_VERSE_INDEX, verseIndex);

            final String verse = paragraphArray.getString(verseIndex);
            if (!hasNonEmptyVerse && !TextUtils.isEmpty(verse))
                hasNonEmptyVerse = true;
            versesValues.put(DatabaseHelper.COLUMN_TEXT, verse);

            db.insert(translationShortName, null, versesValues);
        }
        if (!hasNonEmptyVerse) {
            throw new Exception(String.format("Empty chapter: %s %d-%d",
                    translationShortName, bookIndex, chapterIndex));
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
            db.insert(DatabaseHelper.TABLE_TRANSLATIONS, null, values);
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
