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

package net.zionsoft.obadiah.model.domain;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.auto.value.AutoValue;

import net.zionsoft.auto.droid.ColumnName;

@AutoValue
public abstract class TranslationInfo {
    public static class ColumnNames {
        public static final String NAME = "COLUMN_TRANSLATION_NAME";
        public static final String SHORT_NAME = "COLUMN_TRANSLATION_SHORTNAME";
        public static final String LANGUAGE = "COLUMN_TRANSLATION_LANGUAGE";
        public static final String BLOB_KEY = "COLUMN_TRANSLATION_BLOB_KEY";
        public static final String SIZE = "COLUMN_TRANSLATION_SIZE";
    }

    @ColumnName(ColumnNames.NAME)
    public abstract String name();

    @ColumnName(ColumnNames.SHORT_NAME)
    public abstract String shortName();

    @ColumnName(ColumnNames.LANGUAGE)
    public abstract String language();

    @ColumnName(ColumnNames.BLOB_KEY)
    public abstract String blobKey();

    @ColumnName(ColumnNames.SIZE)
    public abstract int size();

    public abstract ContentValues toContentValues(ContentValues contentValues);

    public static TranslationInfo create(String name, String shortName, String language, String blobKey, int size) {
        return new AutoValue_TranslationInfo(name, shortName, language, blobKey, size);
    }

    public static TranslationInfo create(Cursor cursor) {
        return AutoValue_TranslationInfo.createFromCursor(cursor);
    }
}
