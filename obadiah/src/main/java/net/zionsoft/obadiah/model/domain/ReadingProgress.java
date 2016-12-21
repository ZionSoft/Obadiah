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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReadingProgress {
    public static class ReadChapter {
        public final int book;
        public final int chapter;
        public final long timestamp;

        public ReadChapter(int book, int chapter, long timestamp) {
            this.book = book;
            this.chapter = chapter;
            this.timestamp = timestamp;
        }
    }

    private final int totalChaptersRead;
    private final int finishedBooks;
    private final int finishedOldTestament;
    private final int finishedNewTestament;
    private final int continuousReading;
    private final long lastReadingTimestamp;

    private final SparseArray<List<ReadChapter>> chaptersReadPerBook = new SparseArray<>(Bible.getBookCount());

    public ReadingProgress(List<ReadingProgress.ReadChapter> readChapters, int continuousReading, long lastReadingTimestamp) {
        this.totalChaptersRead = readChapters.size();
        this.continuousReading = continuousReading;
        this.lastReadingTimestamp = lastReadingTimestamp;

        for (int i = totalChaptersRead - 1; i >= 0; --i) {
            final ReadingProgress.ReadChapter readChapter = readChapters.get(i);

            List<ReadChapter> chaptersByBook = chaptersReadPerBook.get(readChapter.book);
            if (chaptersByBook == null) {
                chaptersByBook = new ArrayList<>();
                chaptersReadPerBook.put(readChapter.book, chaptersByBook);
            }
            chaptersByBook.add(readChapter);
        }

        int finishedBooks = 0;
        int finishedOldTestament = 0;
        int finishedNewTestament = 0;
        for (int i = Bible.getBookCount() - 1; i >= 0; --i) {
            final List<ReadChapter> chaptersByBook = chaptersReadPerBook.get(i);
            final int count = chaptersByBook != null ? chaptersByBook.size() : 0;
            if (count == Bible.getChapterCount(i)) {
                ++finishedBooks;
                if (i < Bible.getOldTestamentBookCount()) {
                    ++finishedOldTestament;
                } else {
                    ++finishedNewTestament;
                }
            }
        }
        this.finishedBooks = finishedBooks;
        this.finishedOldTestament = finishedOldTestament;
        this.finishedNewTestament = finishedNewTestament;
    }

    public int getFinishedBooksCount() {
        return finishedBooks;
    }

    public int getFinishedOldTestamentCount() {
        return finishedOldTestament;
    }

    public int getFinishedNewTestamentCount() {
        return finishedNewTestament;
    }

    public int getTotalChapterRead() {
        return totalChaptersRead;
    }

    @Nullable
    public ReadChapter getLastReadChapter(int book) {
        final List<ReadChapter> readChapters = getReadChapters(book);
        final int count = readChapters.size();
        if (count == 0) {
            return null;
        }
        ReadChapter lastReadChapter = readChapters.get(0);
        for (int i = 1; i < count; ++i) {
            final ReadChapter readChapter = readChapters.get(i);
            if (readChapter.timestamp > lastReadChapter.timestamp) {
                lastReadChapter = readChapter;
            }
        }
        return lastReadChapter;
    }

    @NonNull
    public List<ReadChapter> getReadChapters(int book) {
        return chaptersReadPerBook.get(book, Collections.<ReadChapter>emptyList());
    }

    public int getChapterRead(int book) {
        return getReadChapters(book).size();
    }

    public int getChapterCount(int book) {
        return Bible.getChapterCount(book);
    }

    public int getContinuousReadingDays() {
        return continuousReading;
    }

    public long getLastReadingTimestamp() {
        return lastReadingTimestamp;
    }
}
