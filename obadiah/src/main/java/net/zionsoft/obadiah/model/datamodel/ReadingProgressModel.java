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

package net.zionsoft.obadiah.model.datamodel;

import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateUtils;
import android.util.SparseArray;

import com.crashlytics.android.Crashlytics;

import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.database.MetadataTableHelper;
import net.zionsoft.obadiah.model.database.ReadingProgressTableHelper;
import net.zionsoft.obadiah.model.domain.ReadingProgress;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscriber;

@Singleton
public class ReadingProgressModel {
    private final DatabaseHelper databaseHelper;

    @Inject
    public ReadingProgressModel(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public Observable<ReadingProgress> loadReadingProgress() {
        return Observable.create(new Observable.OnSubscribe<ReadingProgress>() {
            @Override
            public void call(Subscriber<? super ReadingProgress> subscriber) {
                final SQLiteDatabase database = databaseHelper.getDatabase();
                try {
                    database.beginTransaction();
                    final List<SparseArray<Long>> chaptersReadPerBook
                            = ReadingProgressTableHelper.getChaptersReadPerBook(database);
                    final int continuousReadingDays = Integer.parseInt(MetadataTableHelper.getMetadata(
                            database, MetadataTableHelper.KEY_CONTINUOUS_READING_DAYS, "1"));
                    database.setTransactionSuccessful();

                    subscriber.onNext(new ReadingProgress(chaptersReadPerBook, continuousReadingDays));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    Crashlytics.getInstance().core.logException(e);
                    subscriber.onError(e);
                } finally {
                    if (database.inTransaction()) {
                        database.endTransaction();
                    }
                }
            }
        });
    }

    public Observable<Void> trackReadingProgress(final int book, final int chapter) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                final SQLiteDatabase database = databaseHelper.getDatabase();
                try {
                    database.beginTransaction();

                    final long now = System.currentTimeMillis();
                    ReadingProgressTableHelper.saveChapterReading(database, book, chapter, now);
                    MetadataTableHelper.saveMetadata(database,
                            MetadataTableHelper.KEY_LAST_READING_TIMESTAMP, Long.toString(now));

                    final long lastReadingDay = Long.parseLong(MetadataTableHelper.getMetadata(
                            database, MetadataTableHelper.KEY_LAST_READING_TIMESTAMP, "0")) / DateUtils.DAY_IN_MILLIS;
                    final long today = now / DateUtils.DAY_IN_MILLIS;
                    final long diff = today - lastReadingDay;
                    int continuousReadingDays = 1;
                    if (diff == 1) {
                        continuousReadingDays = 1 + Integer.parseInt(
                                MetadataTableHelper.getMetadata(database,
                                        MetadataTableHelper.KEY_CONTINUOUS_READING_DAYS, "0"));
                    }
                    MetadataTableHelper.saveMetadata(database,
                            MetadataTableHelper.KEY_CONTINUOUS_READING_DAYS,
                            Integer.toString(continuousReadingDays));

                    database.setTransactionSuccessful();
                } catch (Exception e) {
                    Crashlytics.getInstance().core.logException(e);
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
