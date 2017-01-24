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

package net.zionsoft.obadiah.biblereading.verse;

import net.zionsoft.obadiah.model.domain.Bible;

class VerseHelper {
    static int positionToBookIndex(int position) throws IllegalArgumentException {
        if (position < 0) {
            throw new IllegalArgumentException("Invalid position: " + position);
        }

        final int bookCount = Bible.getBookCount();
        for (int bookIndex = 0; bookIndex < bookCount; ++bookIndex) {
            final int chapterCount = Bible.getChapterCount(bookIndex);
            position -= chapterCount;
            if (position < 0) {
                return bookIndex;
            }
        }

        throw new IllegalArgumentException("Invalid position: " + position);
    }

    static int positionToChapterIndex(int position) throws IllegalArgumentException {
        if (position < 0) {
            throw new IllegalArgumentException("Invalid position: " + position);
        }

        final int bookCount = Bible.getBookCount();
        for (int bookIndex = 0; bookIndex < bookCount; ++bookIndex) {
            final int chapterCount = Bible.getChapterCount(bookIndex);
            if (position < chapterCount) {
                return position;
            }
            position -= chapterCount;
        }

        throw new IllegalArgumentException("Invalid position: " + position);
    }

    static int indexToPosition(int bookIndex, int chapterIndex) throws IllegalArgumentException {
        final int bookCount = Bible.getBookCount();
        if (bookIndex < 0 || bookIndex >= bookCount) {
            throw new IllegalArgumentException("Invalid book index: " + bookIndex);
        }
        final int chapterCount = Bible.getChapterCount(bookIndex);
        if (chapterIndex < 0 || chapterIndex >= chapterCount) {
            throw new IllegalArgumentException("Invalid chapter index: " + chapterIndex);
        }

        int position = 0;
        for (int i = 0; i < bookIndex; ++i) {
            position += Bible.getChapterCount(i);
        }
        return position + chapterIndex;
    }
}
