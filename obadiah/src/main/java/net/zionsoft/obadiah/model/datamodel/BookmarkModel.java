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

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Pair;

import net.zionsoft.obadiah.model.database.BookmarkTableHelper;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.VerseIndex;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Single;
import rx.functions.Func0;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

@Singleton
public class BookmarkModel {
    public static final int ACTION_ADD = 0;
    public static final int ACTION_REMOVE = 1;

    @IntDef({ACTION_ADD, ACTION_REMOVE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Action {
    }

    @SuppressWarnings("WeakerAccess")
    final SerializedSubject<Pair<Integer, Bookmark>, Pair<Integer, Bookmark>> bookmarksUpdatesSubject
            = PublishSubject.<Pair<Integer, Bookmark>>create().toSerialized();

    @SuppressWarnings("WeakerAccess")
    final DatabaseHelper databaseHelper;

    @Inject
    public BookmarkModel(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    @NonNull
    public Observable<Pair<Integer, Bookmark>> observeBookmarks() {
        return bookmarksUpdatesSubject.asObservable();
    }

    public Single<Bookmark> addBookmark(final VerseIndex verseIndex) {
        return Single.fromCallable(new Callable<Bookmark>() {
            @Override
            public Bookmark call() throws Exception {
                final Bookmark bookmark = Bookmark.create(verseIndex, System.currentTimeMillis());
                BookmarkTableHelper.saveBookmark(databaseHelper.getDatabase(), bookmark);
                bookmarksUpdatesSubject.onNext(new Pair<>(ACTION_ADD, bookmark));
                return bookmark;
            }
        });
    }

    public Observable<Void> removeBookmark(final VerseIndex verseIndex) {
        return Observable.defer(new Func0<Observable<Void>>() {
            @Override
            public Observable<Void> call() {
                try {
                    BookmarkTableHelper.removeBookmark(databaseHelper.getDatabase(), verseIndex);
                    bookmarksUpdatesSubject.onNext(new Pair<>(ACTION_REMOVE, Bookmark.create(verseIndex, -1L)));
                    return Observable.empty();
                } catch (Exception e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<List<Bookmark>> loadBookmarks(final int book, final int chapter) {
        return Observable.fromCallable(new Callable<List<Bookmark>>() {
            @Override
            public List<Bookmark> call() throws Exception {
                return BookmarkTableHelper.getBookmarks(databaseHelper.getDatabase(), book, chapter);
            }
        });
    }

    public Single<List<Bookmark>> loadBookmarks() {
        return Single.fromCallable(new Callable<List<Bookmark>>() {
            @Override
            public List<Bookmark> call() throws Exception {
                return BookmarkTableHelper.getBookmarks(databaseHelper.getDatabase());
            }
        });
    }
}
