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

package net.zionsoft.obadiah.model.domain;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcelable;

import com.google.auto.value.AutoValue;

import net.zionsoft.auto.droid.ColumnName;

@AutoValue
public abstract class VerseIndex implements Parcelable {
    public static class ColumnNames {
        public static final String BOOK_INDEX = "COLUMN_BOOK_INDEX";
        public static final String CHAPTER_INDEX = "COLUMN_CHAPTER_INDEX";
        public static final String VERSE_INDEX = "COLUMN_VERSE_INDEX";
    }

    @ColumnName(ColumnNames.BOOK_INDEX)
    public abstract int book();

    @ColumnName(ColumnNames.CHAPTER_INDEX)
    public abstract int chapter();

    @ColumnName(ColumnNames.VERSE_INDEX)
    public abstract int verse();

    public abstract ContentValues toContentValues(ContentValues contentValues);

    public static VerseIndex create(int book, int chapter, int verse) {
        return new AutoValue_VerseIndex(book, chapter, verse);
    }

    public static VerseIndex create(Cursor cursor) {
        return AutoValue_VerseIndex.createFromCursor(cursor);
    }
}
