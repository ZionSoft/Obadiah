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

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class VerseIndex implements Parcelable {
    public abstract int book();

    public abstract int chapter();

    public abstract int verse();

    public static VerseIndex create(int book, int chapter, int verse) {
        return new AutoValue_VerseIndex(book, chapter, verse);
    }
}
