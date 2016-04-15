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

import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.BookmarkTableHelper;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.VerseIndex;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscriber;

@Singleton
public class BookmarkModel {
    private final DatabaseHelper databaseHelper;

    @Inject
    public BookmarkModel(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public Observable<Bookmark> addBookmark(final VerseIndex verseIndex) {
        return Observable.create(new Observable.OnSubscribe<Bookmark>() {
            @Override
            public void call(Subscriber<? super Bookmark> subscriber) {
                try {
                    final Bookmark bookmark = Bookmark.create(verseIndex, System.currentTimeMillis());
                    BookmarkTableHelper.saveBookmark(databaseHelper.getDatabase(), bookmark);
                    Analytics.trackEvent(Analytics.CATEGORY_BOOKMARKS, Analytics.BOOKMARKS_ACTION_ADDED);
                    subscriber.onNext(bookmark);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<Void> removeBookmark(final VerseIndex verseIndex) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    BookmarkTableHelper.removeBookmark(databaseHelper.getDatabase(), verseIndex);
                    Analytics.trackEvent(Analytics.CATEGORY_BOOKMARKS, Analytics.BOOKMARKS_ACTION_REMOVED);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<List<Bookmark>> loadBookmarks(final int book, final int chapter) {
        return Observable.create(new Observable.OnSubscribe<List<Bookmark>>() {
            @Override
            public void call(Subscriber<? super List<Bookmark>> subscriber) {
                try {
                    subscriber.onNext(BookmarkTableHelper.getBookmarks(
                            databaseHelper.getDatabase(), book, chapter));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<List<Bookmark>> loadBookmarks() {
        return Observable.create(new Observable.OnSubscribe<List<Bookmark>>() {
            @Override
            public void call(Subscriber<? super List<Bookmark>> subscriber) {
                try {
                    subscriber.onNext(BookmarkTableHelper.getBookmarks(databaseHelper.getDatabase()));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }
}
