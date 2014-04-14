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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.text.format.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class ReadingProgressManager {
    public static interface OnReadingProgressLoadedListener {
        public void onReadingProgressLoaded(ReadingProgress readingProgress);
    }

    private static ReadingProgressManager sInstance;

    private final DatabaseHelper mDatabaseHelper;

    public static void initialize(Context context) {
        if (sInstance == null) {
            synchronized (Bible.class) {
                if (sInstance == null)
                    sInstance = new ReadingProgressManager(context.getApplicationContext());
            }
        }
    }

    private ReadingProgressManager(Context context) {
        super();

        mDatabaseHelper = DatabaseHelper.getInstance(context);
    }

    public static ReadingProgressManager getInstance() {
        return sInstance;
    }

    public void trackChapterReading(final int book, final int chapter) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                synchronized (mDatabaseHelper) {
                    SQLiteDatabase db = null;
                    try {
                        db = mDatabaseHelper.getWritableDatabase();
                        if (db == null)
                            return null;
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
                            if (db.inTransaction())
                                db.endTransaction();
                            db.close();
                        }
                    }
                }
                return null;
            }
        }.execute();
    }

    public void loadReadingProgress(final OnReadingProgressLoadedListener listener) {
        new AsyncTask<Void, Void, ReadingProgress>() {
            @Override
            protected ReadingProgress doInBackground(Void... params) {
                synchronized (mDatabaseHelper) {
                    SQLiteDatabase db = null;
                    Cursor cursor = null;
                    try {
                        db = mDatabaseHelper.getReadableDatabase();
                        if (db == null)
                            return null;

                        cursor = db.query(DatabaseHelper.TABLE_READING_PROGRESS,
                                new String[]{DatabaseHelper.COLUMN_BOOK_INDEX}, null, null, null, null, null);
                        final int bookIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_BOOK_INDEX);
                        final int[] chaptersReadPerBook = new int[Bible.getBookCount()];
                        while (cursor.moveToNext()) {
                            ++chaptersReadPerBook[cursor.getInt(bookIndex)];
                        }
                        final List<Integer> list = new ArrayList<Integer>(chaptersReadPerBook.length);
                        for (int chaptersRead : chaptersReadPerBook)
                            list.add(chaptersRead);

                        final int continuousReadingDays = Integer.parseInt(
                                DatabaseHelper.getMetadata(db, DatabaseHelper.KEY_CONTINUOUS_READING_DAYS, "1"));

                        return new ReadingProgress(list, continuousReadingDays);
                    } finally {
                        if (cursor != null)
                            cursor.close();
                        if (db != null)
                            db.close();
                    }
                }
            }

            @Override
            protected void onPostExecute(ReadingProgress result) {
                listener.onReadingProgressLoaded(result);
            }
        }.execute();
    }
}
