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

package net.zionsoft.obadiah.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.SparseArray;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.utils.SimpleAsyncTask;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ReadingProgressManager {
    @Inject
    DatabaseHelper databaseHelper;

    @Inject
    public ReadingProgressManager(Context context) {
        App.get(context).getInjectionComponent().inject(this);
    }

    public void trackChapterReading(final int book, final int chapter) {
        new SimpleAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                SQLiteDatabase db = null;
                try {
                    db = databaseHelper.openDatabase();
                    if (db == null) {
                        Analytics.trackException("Failed to open database.");
                        return null;
                    }
                    db.beginTransaction();

                    final ContentValues values = new ContentValues(3);
                    values.put(DatabaseHelper.COLUMN_BOOK_INDEX, book);
                    values.put(DatabaseHelper.COLUMN_CHAPTER_INDEX, chapter);
                    values.put(DatabaseHelper.COLUMN_LAST_READING_TIMESTAMP, System.currentTimeMillis());
                    db.insertWithOnConflict(DatabaseHelper.TABLE_READING_PROGRESS,
                            null, values, SQLiteDatabase.CONFLICT_REPLACE);

                    final long lastReadingDay = Long.parseLong(DatabaseHelper.getMetadata(
                            db, DatabaseHelper.KEY_LAST_READING_TIMESTAMP, "0")) / DateUtils.DAY_IN_MILLIS;
                    final long now = System.currentTimeMillis();
                    final long today = now / DateUtils.DAY_IN_MILLIS;
                    final long diff = today - lastReadingDay;
                    if (diff == 1) {
                        final int continuousReadingDays = Integer.parseInt(
                                DatabaseHelper.getMetadata(db, DatabaseHelper.KEY_CONTINUOUS_READING_DAYS, "0"));
                        DatabaseHelper.setMetadata(db, DatabaseHelper.KEY_CONTINUOUS_READING_DAYS,
                                Integer.toString(continuousReadingDays + 1));
                    } else if (diff > 2) {
                        DatabaseHelper.setMetadata(db, DatabaseHelper.KEY_CONTINUOUS_READING_DAYS, "1");
                    }
                    DatabaseHelper.setMetadata(db, DatabaseHelper.KEY_LAST_READING_TIMESTAMP, Long.toString(now));

                    db.setTransactionSuccessful();
                } finally {
                    if (db != null) {
                        if (db.inTransaction()) {
                            db.endTransaction();
                        }
                        databaseHelper.closeDatabase();
                    }
                }
                return null;
            }
        }.start();
    }

    @Nullable
    public ReadingProgress loadReadingProgress() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = databaseHelper.openDatabase();
            if (db == null) {
                Analytics.trackException("Failed to open database.");
                return null;
            }

            cursor = db.query(DatabaseHelper.TABLE_READING_PROGRESS,
                    new String[]{DatabaseHelper.COLUMN_BOOK_INDEX, DatabaseHelper.COLUMN_CHAPTER_INDEX,
                            DatabaseHelper.COLUMN_LAST_READING_TIMESTAMP},
                    null, null, null, null, null
            );
            final int bookIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_BOOK_INDEX);
            final int chapterIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CHAPTER_INDEX);
            final int lastReadingTimestamp = cursor.getColumnIndex(DatabaseHelper.COLUMN_LAST_READING_TIMESTAMP);

            final int bookCount = Bible.getBookCount();
            final List<SparseArray<Long>> chaptersReadPerBook = new ArrayList<>(bookCount);
            for (int i = 0; i < bookCount; ++i)
                chaptersReadPerBook.add(new SparseArray<Long>(Bible.getChapterCount(i)));
            while (cursor.moveToNext()) {
                chaptersReadPerBook.get(cursor.getInt(bookIndex))
                        .append(cursor.getInt(chapterIndex), cursor.getLong(lastReadingTimestamp));
            }

            final int continuousReadingDays = Integer.parseInt(
                    DatabaseHelper.getMetadata(db, DatabaseHelper.KEY_CONTINUOUS_READING_DAYS, "1"));

            return new ReadingProgress(chaptersReadPerBook, continuousReadingDays);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                databaseHelper.closeDatabase();
            }
        }
    }
}
