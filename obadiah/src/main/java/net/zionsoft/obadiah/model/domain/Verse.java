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

import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public class Verse {
    public static class Text {
        public final String translation;
        public final String bookName;
        public final String text;

        public Text(String translation, String bookName, String text) {
            this.translation = translation;
            this.bookName = bookName;
            this.text = text;
        }
    }

    public final VerseIndex verseIndex;
    public final Text text;
    public final List<Text> parallel;

    public Verse(VerseIndex verseIndex, Text text, @Nullable List<Text> parallel) {
        super();

        this.verseIndex = verseIndex;
        this.text = text;
        this.parallel = parallel != null ? parallel : Collections.<Text>emptyList();
    }
}
