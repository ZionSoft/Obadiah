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

public class Bible {
    private static final int BOOK_COUNT = 66;
    private static final int OLD_TESTAMENT_COUNT = 39;
    private static final int NEW_TESTAMENT_COUNT = 27;
    private static final int TOTAL_CHAPTER_COUNT = 1189;
    private static final int[] CHAPTER_COUNT = {50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36,
            10, 13, 10, 42, 150, 31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4,
            28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22};

    public static int getBookCount() {
        return BOOK_COUNT;
    }

    public static int getOldTestamentBookCount() {
        return OLD_TESTAMENT_COUNT;
    }

    public static int getNewTestamentBookCount() {
        return NEW_TESTAMENT_COUNT;
    }

    public static int getTotalChapterCount() {
        return TOTAL_CHAPTER_COUNT;
    }

    public static int getChapterCount(int book) {
        return CHAPTER_COUNT[book];
    }
}
