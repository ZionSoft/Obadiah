/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2014 ZionSoft
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
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.TranslationInfo;
import net.zionsoft.obadiah.model.database.DatabaseHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TranslationHelper {
    public static List<TranslationInfo> toTranslationList(JSONArray jsonArray) throws Exception {
        final int length = jsonArray.length();
        final List<TranslationInfo> translations = new ArrayList<TranslationInfo>(length);
        for (int i = 0; i < length; ++i) {
            final JSONObject translationObject = jsonArray.getJSONObject(i);
            final String name = translationObject.getString("name");
            final String shortName = translationObject.getString("shortName");
            final String language = translationObject.getString("language");
            final int size = translationObject.getInt("size");
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(shortName)
                    || TextUtils.isEmpty(language) || size <= 0) {
                throw new Exception("Illegal translation info.");
            }
            translations.add(new TranslationInfo(name, shortName, language, size));
        }
        return translations;
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
}
