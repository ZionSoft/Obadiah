/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2013 ZionSoft
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

package net.zionsoft.obadiah.bible;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class TranslationManager {
    public TranslationManager(Context context) {
        super();
        mTranslationsDatabaseHelper = new TranslationsDatabaseHelper(context);
    }

    public void addTranslations(List<TranslationInfo> translations) {
        if (translations == null || translations.size() == 0)
            return;

        // excludes existing translations
        final List<TranslationInfo> existingTranslations = translations(TRANSLATIONS_ALL);
        List<TranslationInfo> newTranslations;
        if (existingTranslations.size() == 0) {
            newTranslations = translations;
        } else {
            newTranslations = new ArrayList<TranslationInfo>(translations.size());
            for (TranslationInfo translation : translations) {
                boolean newTranslation = true;
                for (TranslationInfo existing : existingTranslations) {
                    if (translation.shortName.equals(existing.shortName)) {
                        newTranslation = false;
                        break;
                    }
                }
                if (newTranslation)
                    newTranslations.add(translation);
            }
        }
        if (newTranslations.size() == 0)
            return;

        // adds new translations to database
        SQLiteDatabase db = null;
        try {
            db = mTranslationsDatabaseHelper.getWritableDatabase();
            db.beginTransaction();

            final ContentValues values = new ContentValues(3);
            for (TranslationInfo translationInfo : newTranslations) {
                values.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_ID,
                        translationInfo.uniqueId);

                values.put(TranslationsDatabaseHelper.COLUMN_KEY,
                        TranslationsDatabaseHelper.KEY_NAME);
                values.put(TranslationsDatabaseHelper.COLUMN_VALUE, translationInfo.name);
                db.insert(TranslationsDatabaseHelper.TABLE_TRANSLATION_LIST, null, values);

                values.put(TranslationsDatabaseHelper.COLUMN_KEY,
                        TranslationsDatabaseHelper.KEY_SHORT_NAME);
                values.put(TranslationsDatabaseHelper.COLUMN_VALUE, translationInfo.shortName);
                db.insert(TranslationsDatabaseHelper.TABLE_TRANSLATION_LIST, null, values);

                values.put(TranslationsDatabaseHelper.COLUMN_KEY,
                        TranslationsDatabaseHelper.KEY_LANGUAGE);
                values.put(TranslationsDatabaseHelper.COLUMN_VALUE, translationInfo.language);
                db.insert(TranslationsDatabaseHelper.TABLE_TRANSLATION_LIST, null, values);

                values.put(TranslationsDatabaseHelper.COLUMN_KEY,
                        TranslationsDatabaseHelper.KEY_BLOB_KEY);
                values.put(TranslationsDatabaseHelper.COLUMN_VALUE, translationInfo.blobKey);
                db.insert(TranslationsDatabaseHelper.TABLE_TRANSLATION_LIST, null, values);

                values.put(TranslationsDatabaseHelper.COLUMN_KEY,
                        TranslationsDatabaseHelper.KEY_SIZE);
                values.put(TranslationsDatabaseHelper.COLUMN_VALUE,
                        Integer.toString(translationInfo.size));
                db.insert(TranslationsDatabaseHelper.TABLE_TRANSLATION_LIST, null, values);

                values.put(TranslationsDatabaseHelper.COLUMN_KEY,
                        TranslationsDatabaseHelper.KEY_TIMESTAMP);
                values.put(TranslationsDatabaseHelper.COLUMN_VALUE,
                        Long.toString(translationInfo.timestamp));
                db.insert(TranslationsDatabaseHelper.TABLE_TRANSLATION_LIST, null, values);

                values.put(TranslationsDatabaseHelper.COLUMN_KEY,
                        TranslationsDatabaseHelper.KEY_INSTALLED);
                values.put(TranslationsDatabaseHelper.COLUMN_VALUE,
                        Boolean.toString(translationInfo.installed));
                db.insert(TranslationsDatabaseHelper.TABLE_TRANSLATION_LIST, null, values);
            }

            db.setTransactionSuccessful();
        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    public void removeTranslation(TranslationInfo translation) {
        if (translation == null)
            throw new IllegalArgumentException();

        SQLiteDatabase db = null;
        try {
            db = mTranslationsDatabaseHelper.getWritableDatabase();
            db.beginTransaction();

            // deletes the translation table
            db.execSQL(String.format("DROP TABLE IF EXISTS %s", translation.shortName));

            // deletes the book names
            db.delete(TranslationsDatabaseHelper.TABLE_BOOK_NAMES,
                    String.format("%s = ?", TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME),
                    new String[]{translation.shortName});

            // sets the translation as "not installed"
            final ContentValues values = new ContentValues(1);
            values.put(TranslationsDatabaseHelper.COLUMN_VALUE, Boolean.toString(false));
            db.update(TranslationsDatabaseHelper.TABLE_TRANSLATION_LIST, values,
                    String.format("%s = ? AND %s = ?",
                            TranslationsDatabaseHelper.COLUMN_TRANSLATION_ID,
                            TranslationsDatabaseHelper.COLUMN_KEY),
                    new String[]{Long.toString(translation.uniqueId),
                            TranslationsDatabaseHelper.KEY_INSTALLED});

            db.setTransactionSuccessful();
        } finally {
            if (db != null) {
                db.endTransaction();
                db.close();
            }
        }
    }

    public List<TranslationInfo> installedTranslations() {
        return translations(TRANSLATIONS_INSTALLED);
    }

    public List<TranslationInfo> availableTranslations() {
        return translations(TRANSLATIONS_AVAILABLE);
    }

    private List<TranslationInfo> translations(int criteria) {
        SQLiteDatabase db = null;
        try {
            db = mTranslationsDatabaseHelper.getReadableDatabase();
            List<TranslationInfo> allTranslations = null;
            final Cursor cursor = db.query(TranslationsDatabaseHelper.TABLE_TRANSLATION_LIST,
                    new String[]{TranslationsDatabaseHelper.COLUMN_TRANSLATION_ID,
                            TranslationsDatabaseHelper.COLUMN_KEY,
                            TranslationsDatabaseHelper.COLUMN_VALUE},
                    null, null, null, null,
                    String.format("%s ASC", TranslationsDatabaseHelper.COLUMN_TRANSLATION_ID));
            if (cursor != null) {
                allTranslations = new ArrayList<TranslationInfo>(cursor.getCount());
                final int translationIdIndex
                        = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_TRANSLATION_ID);
                final int keyIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_KEY);
                final int valueIndex
                        = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_VALUE);

                long uniqueId;
                String name;
                String shortName;
                String language;
                String blobKey;
                int size;
                long timestamp;
                boolean isInstalled;
                if (cursor.moveToNext()) {
                    do {
                        uniqueId = cursor.getLong(translationIdIndex);
                        name = null;
                        shortName = null;
                        language = null;
                        blobKey = null;
                        size = -1;
                        timestamp = -1L;
                        isInstalled = false;

                        while (uniqueId == cursor.getLong(translationIdIndex)) {
                            String key = cursor.getString(keyIndex);
                            String value = cursor.getString(valueIndex);
                            if (key.equals(TranslationsDatabaseHelper.KEY_NAME))
                                name = value;
                            else if (key.equals(TranslationsDatabaseHelper.KEY_SHORT_NAME))
                                shortName = value;
                            else if (key.equals(TranslationsDatabaseHelper.KEY_LANGUAGE))
                                language = value;
                            else if (key.equals(TranslationsDatabaseHelper.KEY_BLOB_KEY))
                                blobKey = value;
                            else if (key.equals(TranslationsDatabaseHelper.KEY_SIZE))
                                size = Integer.parseInt(value);
                            else if (key.equals(TranslationsDatabaseHelper.KEY_TIMESTAMP))
                                timestamp = Long.parseLong(value);
                            else if (key.equals(TranslationsDatabaseHelper.KEY_INSTALLED))
                                isInstalled = Boolean.parseBoolean(value);

                            if (!cursor.moveToNext())
                                break;
                        }

                        allTranslations.add(new TranslationInfo(uniqueId, name, shortName, language,
                                blobKey, size, timestamp, isInstalled));

                        if (cursor.isAfterLast())
                            break;
                    } while (true);
                }
            }

            if (criteria == TRANSLATIONS_ALL) {
                return allTranslations;
            } else {
                List<TranslationInfo> translations
                        = new ArrayList<TranslationInfo>(allTranslations.size());
                for (TranslationInfo translationInfo : allTranslations) {
                    if ((criteria == TRANSLATIONS_INSTALLED && translationInfo.installed)
                            || (criteria == TRANSLATIONS_AVAILABLE && !translationInfo.installed)) {
                        translations.add(translationInfo);
                    }
                }
                return translations;
            }
        } finally {
            if (db != null)
                db.close();
        }
    }

    private static final int TRANSLATIONS_ALL = 0;
    private static final int TRANSLATIONS_AVAILABLE = 1;
    private static final int TRANSLATIONS_INSTALLED = 2;

    private final TranslationsDatabaseHelper mTranslationsDatabaseHelper;
}
