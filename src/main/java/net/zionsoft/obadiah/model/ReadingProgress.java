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

package net.zionsoft.obadiah.model;

import java.util.List;

public class ReadingProgress {
    private final int mTotalChaptersRead;
    private final int mFinishedBooks;
    private final int mFinishedOldTestament;
    private final int mFinishedNewTestament;
    private final int mContinuousReading;
    private final List<Integer> mChaptersReadPerBook;

    ReadingProgress(List<Integer> chaptersReadPerBook, int continuousReading) {
        super();

        int count = 0;
        int finishedBooks = 0;
        int finishedOldTestament = 0;
        int finishedNewTestament = 0;
        int i = 0;
        for (Integer chaptersRead : chaptersReadPerBook) {
            count += chaptersRead;

            if (chaptersRead == Bible.getChapterCount(i)) {
                ++finishedBooks;
                if (i < 39)
                    ++finishedOldTestament;
                else
                    ++finishedNewTestament;
            }
            ++i;
        }
        mTotalChaptersRead = count;
        mFinishedBooks = finishedBooks;
        mFinishedOldTestament = finishedOldTestament;
        mFinishedNewTestament = finishedNewTestament;

        mContinuousReading = continuousReading;
        mChaptersReadPerBook = chaptersReadPerBook;
    }

    public int getFinishedBooksCount() {
        return mFinishedBooks;
    }

    public int getFinishedOldTestamentCount() {
        return mFinishedOldTestament;
    }

    public int getFinishedNewTestamentCount() {
        return mFinishedNewTestament;
    }

    public int getTotalChapterRead() {
        return mTotalChaptersRead;
    }

    public int getChapterRead(int book) {
        return mChaptersReadPerBook.get(book);
    }

    public int getChapterCount(int book) {
        return Bible.getChapterCount(book);
    }

    public int getContinuousReadingDays() {
        return mContinuousReading;
    }
}
