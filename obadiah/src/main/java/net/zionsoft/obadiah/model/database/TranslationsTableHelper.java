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

import net.zionsoft.obadiah.model.domain.TranslationInfo;
import net.zionsoft.obadiah.utils.TextFormatter;

import java.util.ArrayList;
import java.util.List;

public class TranslationsTableHelper {
    private static final String TABLE_TRANSLATIONS = "TABLE_TRANSLATIONS";
    private static final String COLUMN_TRANSLATION_NAME = "COLUMN_TRANSLATION_NAME";
    private static final String COLUMN_TRANSLATION_SHORT_NAME = "COLUMN_TRANSLATION_SHORTNAME";
    private static final String COLUMN_TRANSLATION_LANGUAGE = "COLUMN_TRANSLATION_LANGUAGE";
    private static final String COLUMN_TRANSLATION_BLOB_KEY = "COLUMN_TRANSLATION_BLOB_KEY";
    private static final String COLUMN_TRANSLATION_SIZE = "COLUMN_TRANSLATION_SIZE";

    static void createTable(SQLiteDatabase db) {
        db.execSQL(TextFormatter.format("CREATE TABLE %s (%s TEXT PRIMARY KEY, %s TEXT NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL, %s INTEGER NOT NULL);",
                TABLE_TRANSLATIONS, COLUMN_TRANSLATION_NAME, COLUMN_TRANSLATION_SHORT_NAME,
                COLUMN_TRANSLATION_LANGUAGE, COLUMN_TRANSLATION_BLOB_KEY, COLUMN_TRANSLATION_SIZE));
    }

    public static void saveTranslations(SQLiteDatabase db, List<TranslationInfo> translations) {
        final ContentValues values = new ContentValues(5);
        final int size = translations.size();
        for (int i = 0; i < size; ++i) {
            final TranslationInfo translation = translations.get(i);
            values.put(COLUMN_TRANSLATION_NAME, translation.name);
            values.put(COLUMN_TRANSLATION_SHORT_NAME, translation.shortName);
            values.put(COLUMN_TRANSLATION_LANGUAGE, translation.language);
            values.put(COLUMN_TRANSLATION_BLOB_KEY, translation.blobKey);
            values.put(COLUMN_TRANSLATION_SIZE, translation.size);
            db.insertWithOnConflict(TABLE_TRANSLATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    @NonNull
    public static List<TranslationInfo> getTranslations(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_TRANSLATIONS,
                    new String[]{COLUMN_TRANSLATION_NAME, COLUMN_TRANSLATION_SHORT_NAME,
                            COLUMN_TRANSLATION_LANGUAGE, COLUMN_TRANSLATION_BLOB_KEY,
                            COLUMN_TRANSLATION_SIZE}, null, null, null, null, null, null);
            final int name = cursor.getColumnIndex(COLUMN_TRANSLATION_NAME);
            final int shortName = cursor.getColumnIndex(COLUMN_TRANSLATION_SHORT_NAME);
            final int language = cursor.getColumnIndex(COLUMN_TRANSLATION_LANGUAGE);
            final int blobKey = cursor.getColumnIndex(COLUMN_TRANSLATION_BLOB_KEY);
            final int size = cursor.getColumnIndex(COLUMN_TRANSLATION_SIZE);
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

    @NonNull
    public static List<String> getDownloadedTranslations(SQLiteDatabase db) {
        // TODO can we get this without touching the book names table?
        Cursor cursor = null;
        try {
            cursor = db.query(true, "TABLE_BOOK_NAMES", new String[]{"COLUMN_TRANSLATION_SHORTNAME"},
                    null, null, null, null, null, null);
            final int translationShortName = cursor.getColumnIndex("COLUMN_TRANSLATION_SHORTNAME");
            final List<String> translations = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                translations.add(cursor.getString(translationShortName));
            }
            return translations;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
