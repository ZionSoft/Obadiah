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

import android.support.annotation.NonNull;
import android.util.Pair;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.User;
import net.zionsoft.obadiah.model.domain.VerseIndex;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observer;
import rx.Single;
import rx.Subscription;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

@Singleton
public class SyncModel {
    private static final StringBuilder STRING_BUILDER = new StringBuilder();

    private final BookmarkModel bookmarkModel;

    @SuppressWarnings("WeakerAccess")
    final FirebaseDatabase firebaseDatabase;

    @SuppressWarnings("WeakerAccess")
    DatabaseReference bookmarksReference;
    private Subscription observeBookmarksSubscription;

    @Inject
    public SyncModel(UserModel userModel, BookmarkModel bookmarkModel) {
        this.bookmarkModel = bookmarkModel;

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.setPersistenceEnabled(true);

        userModel.observeCurrentUser().observeOn(Schedulers.io())
                .subscribe(new Observer<User>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(User user) {
                        unsubscribeAll();
                        if (user != null) {
                            initialSync(user);
                        }
                    }
                });
    }

    @SuppressWarnings("WeakerAccess")
    void unsubscribeAll() {
        unsubscribeObserveBookmarks();
    }

    private void unsubscribeObserveBookmarks() {
        if (observeBookmarksSubscription != null) {
            observeBookmarksSubscription.unsubscribe();
            observeBookmarksSubscription = null;
        }
    }

    @SuppressWarnings("WeakerAccess")
    void initialSync(@NonNull User user) {
        syncBookmarks(user);
    }

    private void syncBookmarks(@NonNull final User user) {
        final String path = buildBookMarksRootPath(user);
        bookmarksReference = firebaseDatabase.getReference(path);
        bookmarkModel.loadBookmarks().map(new Func1<List<Bookmark>, Void>() {
            @Override
            public Void call(List<Bookmark> bookmarks) {
                for (int i = bookmarks.size() - 1; i >= 0; --i) {
                    final Bookmark bookmark = bookmarks.get(i);
                    bookmarksReference.child(buildBookMarkKey(bookmark)).setValue(bookmark.timestamp());
                }

                return null;
            }
        }).onErrorResumeNext(Single.<Void>just(null)).subscribeOn(Schedulers.io()).subscribe();

        unsubscribeObserveBookmarks();
        observeBookmarksSubscription = bookmarkModel.observeBookmarks().observeOn(Schedulers.io())
                .subscribe(new Observer<Pair<Integer, Bookmark>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(Pair<Integer, Bookmark> bookmark) {
                        if (bookmarksReference != null) {
                            final DatabaseReference reference
                                    = bookmarksReference.child(buildBookMarkKey(bookmark.second));
                            switch (bookmark.first) {
                                case BookmarkModel.ACTION_ADD:
                                    reference.setValue(bookmark.second.timestamp());
                                    break;
                                case BookmarkModel.ACTION_REMOVE:
                                    reference.removeValue();
                                    break;
                            }
                        }
                    }
                });
    }

    @SuppressWarnings("WeakerAccess")
    static String buildBookMarksRootPath(@NonNull User user) {
        synchronized (STRING_BUILDER) {
            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append("/bookmarks/").append(user.uid);
            return STRING_BUILDER.toString();
        }
    }

    @SuppressWarnings("WeakerAccess")
    static String buildBookMarkKey(@NonNull Bookmark bookmark) {
        synchronized (STRING_BUILDER) {
            STRING_BUILDER.setLength(0);
            final VerseIndex verseIndex = bookmark.verseIndex();
            STRING_BUILDER.append(verseIndex.book()).append(':')
                    .append(verseIndex.chapter()).append(':').append(verseIndex.verse());
            return STRING_BUILDER.toString();
        }
    }
}
