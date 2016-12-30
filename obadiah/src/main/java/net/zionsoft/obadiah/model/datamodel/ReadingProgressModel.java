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

package net.zionsoft.obadiah.model.datamodel;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import net.zionsoft.obadiah.model.crash.Crash;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.database.MetadataTableHelper;
import net.zionsoft.obadiah.model.database.ReadingProgressTableHelper;
import net.zionsoft.obadiah.model.domain.ReadingProgress;
import net.zionsoft.obadiah.utils.Triple;

import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Completable;
import rx.CompletableSubscriber;
import rx.Observable;
import rx.functions.Func0;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

@Singleton
public class ReadingProgressModel {
    @SuppressWarnings("WeakerAccess")
    final DatabaseHelper databaseHelper;

    @SuppressWarnings("WeakerAccess")
    final SerializedSubject<Triple<ReadingProgress.ReadChapter, Integer, Long>, Triple<ReadingProgress.ReadChapter, Integer, Long>> readingProgressUpdatesSubject
            = PublishSubject.<Triple<ReadingProgress.ReadChapter, Integer, Long>>create().toSerialized();

    @Inject
    public ReadingProgressModel(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    @NonNull
    public Observable<Triple<ReadingProgress.ReadChapter, Integer, Long>> observeReadingProgress() {
        return readingProgressUpdatesSubject.asObservable();
    }

    @NonNull
    public Observable<ReadingProgress> loadReadingProgress() {
        return Observable.fromCallable(new Callable<ReadingProgress>() {
            @Override
            public ReadingProgress call() throws Exception {
                final SQLiteDatabase database = databaseHelper.getDatabase();
                try {
                    database.beginTransaction();
                    final List<ReadingProgress.ReadChapter> readChapters
                            = ReadingProgressTableHelper.getChaptersReadPerBook(database);
                    final int continuousReadingDays = Integer.parseInt(MetadataTableHelper.getMetadata(
                            database, MetadataTableHelper.KEY_CONTINUOUS_READING_DAYS, "1"));
                    final long lastReadingTimestamp = Long.parseLong(MetadataTableHelper.getMetadata(
                            database, MetadataTableHelper.KEY_LAST_READING_TIMESTAMP, "0"));
                    database.setTransactionSuccessful();

                    return new ReadingProgress(readChapters, continuousReadingDays, lastReadingTimestamp);
                } finally {
                    if (database.inTransaction()) {
                        database.endTransaction();
                    }
                }
            }
        });
    }

    @NonNull
    public Completable trackReadingProgress(final int book, final int chapter) {
        return trackReadingProgress(book, chapter, System.currentTimeMillis());
    }

    @NonNull
    public Completable trackReadingProgress(final int book, final int chapter, final long timestamp) {
        return Completable.defer(new Func0<Completable>() {
            @Override
            public Completable call() {
                final SQLiteDatabase database = databaseHelper.getDatabase();
                try {
                    database.beginTransaction();

                    long lastReadingTimestamp = Long.parseLong(MetadataTableHelper.getMetadata(
                            database, MetadataTableHelper.KEY_LAST_READING_TIMESTAMP, "0"));
                    if (lastReadingTimestamp >= timestamp
                            || ReadingProgressTableHelper.getChapterReadTimestamp(database, book, chapter) >= timestamp) {
                        return null;
                    }

                    ReadingProgressTableHelper.saveChapterReading(database, book, chapter, timestamp);

                    final long lastReadingDay = lastReadingTimestamp / DateUtils.DAY_IN_MILLIS;
                    final long today = timestamp / DateUtils.DAY_IN_MILLIS;
                    final long diff = today - lastReadingDay;
                    int continuousReadingDays = 1;
                    if (diff == 0L) {
                        continuousReadingDays = Integer.parseInt(
                                MetadataTableHelper.getMetadata(database,
                                        MetadataTableHelper.KEY_CONTINUOUS_READING_DAYS, "0"));
                    } else if (diff == 1L) {
                        continuousReadingDays = 1 + Integer.parseInt(
                                MetadataTableHelper.getMetadata(database,
                                        MetadataTableHelper.KEY_CONTINUOUS_READING_DAYS, "0"));
                    }
                    if (diff >= 1L) {
                        lastReadingTimestamp = timestamp;
                        MetadataTableHelper.saveMetadata(database,
                                MetadataTableHelper.KEY_LAST_READING_TIMESTAMP,
                                Long.toString(lastReadingTimestamp));
                        MetadataTableHelper.saveMetadata(database,
                                MetadataTableHelper.KEY_CONTINUOUS_READING_DAYS,
                                Integer.toString(continuousReadingDays));
                    }

                    database.setTransactionSuccessful();

                    readingProgressUpdatesSubject.onNext(new Triple<>(
                            new ReadingProgress.ReadChapter(book, chapter, timestamp),
                            continuousReadingDays, lastReadingTimestamp));

                    return Completable.complete();
                } catch (Exception e) {
                    Crash.report(e);
                    return Completable.error(e);
                } finally {
                    // yep, we can crash here, ref.
                    // https://fabric.io/zionsoft/android/apps/net.zionsoft.obadiah/issues/5785af31ffcdc042509c9d00
                    try {
                        if (database.inTransaction()) {
                            database.endTransaction();
                        }
                    } catch (Exception e) {
                        Crash.report(e);
                    }
                }
            }
        });
    }

    @NonNull
    Completable updateContinuousReadingDays(final int days) {
        return Completable.create(new Completable.OnSubscribe() {
            @Override
            public void call(CompletableSubscriber subscriber) {
                try {
                    MetadataTableHelper.saveMetadata(databaseHelper.getDatabase(),
                            MetadataTableHelper.KEY_CONTINUOUS_READING_DAYS,
                            Integer.toString(days));
                    subscriber.onCompleted();
                } catch (Throwable e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    @NonNull
    Completable updateLastReadingTimestamp(final long timestamp) {
        return Completable.create(new Completable.OnSubscribe() {
            @Override
            public void call(CompletableSubscriber subscriber) {
                final SQLiteDatabase database = databaseHelper.getDatabase();
                try {
                    database.beginTransaction();
                    final long lastReadingTimestamp = Long.parseLong(MetadataTableHelper.getMetadata(
                            database, MetadataTableHelper.KEY_LAST_READING_TIMESTAMP, "0"));
                    if (timestamp > lastReadingTimestamp) {
                        MetadataTableHelper.saveMetadata(databaseHelper.getDatabase(),
                                MetadataTableHelper.KEY_LAST_READING_TIMESTAMP,
                                Long.toString(timestamp));
                    }
                    database.setTransactionSuccessful();

                    subscriber.onCompleted();
                } catch (Throwable e) {
                    subscriber.onError(e);
                } finally {
                    if (database.inTransaction()) {
                        database.endTransaction();
                    }
                }
            }
        });
    }
}
