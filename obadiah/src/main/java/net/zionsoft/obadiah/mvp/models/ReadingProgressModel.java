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

package net.zionsoft.obadiah.mvp.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateUtils;
import android.util.SparseArray;

import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.ReadingProgress;
import net.zionsoft.obadiah.model.database.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;

public class ReadingProgressModel {
    private final SQLiteDatabase database;

    public ReadingProgressModel(SQLiteDatabase database) {
        this.database = database;
    }

    public Observable<ReadingProgress> loadReadingProgress() {
        return Observable.create(new Observable.OnSubscribe<ReadingProgress>() {
            @Override
            public void call(Subscriber<? super ReadingProgress> subscriber) {
                Cursor cursor = null;
                try {
                    cursor = database.query(DatabaseHelper.TABLE_READING_PROGRESS,
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
                            DatabaseHelper.getMetadata(database, DatabaseHelper.KEY_CONTINUOUS_READING_DAYS, "1"));

                    subscriber.onNext(new ReadingProgress(chaptersReadPerBook, continuousReadingDays));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        });
    }

    public Observable<Void> trackReadingProgress(final int book, final int chapter) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    database.beginTransaction();

                    final ContentValues values = new ContentValues(3);
                    values.put(DatabaseHelper.COLUMN_BOOK_INDEX, book);
                    values.put(DatabaseHelper.COLUMN_CHAPTER_INDEX, chapter);
                    values.put(DatabaseHelper.COLUMN_LAST_READING_TIMESTAMP, System.currentTimeMillis());
                    database.insertWithOnConflict(DatabaseHelper.TABLE_READING_PROGRESS,
                            null, values, SQLiteDatabase.CONFLICT_REPLACE);

                    final long lastReadingDay = Long.parseLong(DatabaseHelper.getMetadata(
                            database, DatabaseHelper.KEY_LAST_READING_TIMESTAMP, "0")) / DateUtils.DAY_IN_MILLIS;
                    final long now = System.currentTimeMillis();
                    final long today = now / DateUtils.DAY_IN_MILLIS;
                    final long diff = today - lastReadingDay;
                    if (diff == 1) {
                        final int continuousReadingDays = Integer.parseInt(
                                DatabaseHelper.getMetadata(database, DatabaseHelper.KEY_CONTINUOUS_READING_DAYS, "0"));
                        DatabaseHelper.setMetadata(database, DatabaseHelper.KEY_CONTINUOUS_READING_DAYS,
                                Integer.toString(continuousReadingDays + 1));
                    } else if (diff > 2) {
                        DatabaseHelper.setMetadata(database, DatabaseHelper.KEY_CONTINUOUS_READING_DAYS, "1");
                    }
                    DatabaseHelper.setMetadata(database, DatabaseHelper.KEY_LAST_READING_TIMESTAMP, Long.toString(now));

                    database.setTransactionSuccessful();
                } catch (Exception e) {
                    // do nothing
                } finally {
                    if (database.inTransaction()) {
                        database.endTransaction();
                    }
                }

                subscriber.onCompleted();
            }
        });
    }
}
