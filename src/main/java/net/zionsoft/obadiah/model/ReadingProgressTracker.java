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
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

public class ReadingProgressTracker {
    private static ReadingProgressTracker sInstance;

    private final DatabaseHelper mDatabaseHelper;

    public static void initialize(Context context) {
        if (sInstance == null) {
            synchronized (Bible.class) {
                if (sInstance == null)
                    sInstance = new ReadingProgressTracker(context.getApplicationContext());
            }
        }
    }

    private ReadingProgressTracker(Context context) {
        super();

        mDatabaseHelper = DatabaseHelper.getInstance(context);
    }

    public static ReadingProgressTracker getInstance() {
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

                        final ContentValues values = new ContentValues(3);
                        values.put(DatabaseHelper.COLUMN_BOOK_INDEX, book);
                        values.put(DatabaseHelper.COLUMN_CHAPTER_INDEX, chapter);
                        values.put(DatabaseHelper.COLUMN_LAST_READING_TIMESTAMP, System.currentTimeMillis());
                        db.insertWithOnConflict(DatabaseHelper.TABLE_READING_PROGRESS,
                                null, values, SQLiteDatabase.CONFLICT_REPLACE);
                    } finally {
                        if (db != null)
                            db.close();
                    }
                }
                return null;
            }
        }.execute();
    }
}
