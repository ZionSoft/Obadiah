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

import android.util.Pair;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

public class ReadingProgress {
    private final int mTotalChaptersRead;
    private final int mFinishedBooks;
    private final int mFinishedOldTestament;
    private final int mFinishedNewTestament;
    private final int mContinuousReading;
    private final List<Integer> mChaptersReadPerBook;
    private final List<Pair<Integer, Long>> mLastChapterReadPerBook;

    ReadingProgress(List<SparseArray<Long>> chaptersReadPerBook, int continuousReading) {
        super();

        int totalChaptersRead = 0;
        int finishedBooks = 0;
        int finishedOldTestament = 0;
        int finishedNewTestament = 0;
        int i = 0;
        mChaptersReadPerBook = new ArrayList<Integer>(Bible.getBookCount());
        mLastChapterReadPerBook = new ArrayList<Pair<Integer, Long>>(Bible.getBookCount());
        for (SparseArray<Long> chaptersRead : chaptersReadPerBook) {
            final int chaptersCount = Bible.getChapterCount(i);
            int lastReadChapter = -1;
            long lastReadTimestamp = 0L;
            for (int j = 0; j < chaptersCount; ++j) {
                final long timestamp = chaptersRead.get(j, 0L);
                if (lastReadTimestamp < timestamp) {
                    lastReadTimestamp = timestamp;
                    lastReadChapter = j;
                }
            }
            mLastChapterReadPerBook.add(new Pair<Integer, Long>(lastReadChapter, lastReadTimestamp));

            final int chaptersReadCount = chaptersRead.size();
            mChaptersReadPerBook.add(chaptersReadCount);
            totalChaptersRead += chaptersReadCount;

            if (chaptersReadCount == Bible.getChapterCount(i)) {
                ++finishedBooks;
                if (i < Bible.getOldTestamentBookCount())
                    ++finishedOldTestament;
                else
                    ++finishedNewTestament;
            }
            ++i;
        }
        mTotalChaptersRead = totalChaptersRead;
        mFinishedBooks = finishedBooks;
        mFinishedOldTestament = finishedOldTestament;
        mFinishedNewTestament = finishedNewTestament;
        mContinuousReading = continuousReading;
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

    public Pair<Integer, Long> getLastReadChapter(int book) {
        return mLastChapterReadPerBook.get(book);
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
