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

package net.zionsoft.obadiah.model.domain;

import java.util.List;

// TODO merge with Verse
public class VerseWithParallelTranslations {
    public static class Text {
        public final String translation;
        public final String text;

        public Text(String translation, String text) {
            this.translation = translation;
            this.text = text;
        }
    }

    public final Verse.Index verseIndex;
    public final List<Text> texts;

    public VerseWithParallelTranslations(Verse.Index verseIndex, List<Text> texts) {
        this.verseIndex = verseIndex;
        this.texts = texts;
    }
}
