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

package net.zionsoft.obadiah.bookmarks;

import android.util.Pair;

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.BookmarkModel;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.mvp.BasePresenter;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

class BookmarksPresenter extends BasePresenter<BookmarksView> {
    private final BibleReadingModel bibleReadingModel;
    private final BookmarkModel bookmarkModel;

    private Subscription subscription;

    BookmarksPresenter(BibleReadingModel bibleReadingModel, BookmarkModel bookmarkModel, Settings settings) {
        super(settings);
        this.bibleReadingModel = bibleReadingModel;
        this.bookmarkModel = bookmarkModel;
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }

    void loadBookmarks() {
        subscription = bookmarkModel.loadBookmarks()
                .map(new Func1<List<Bookmark>, Pair<List<Bookmark>, List<Verse>>>() {
                    @Override
                    public Pair<List<Bookmark>, List<Verse>> call(List<Bookmark> bookmarks) {
                        final int count = bookmarks.size();
                        final List<Verse> verses = new ArrayList<>(count);
                        final String translation = bibleReadingModel.loadCurrentTranslation();
                        for (int i = 0; i < count; ++i) {
                            final VerseIndex verseIndex = bookmarks.get(i).verseIndex;
                            verses.add(bibleReadingModel.loadVerse(translation, verseIndex.book,
                                    verseIndex.chapter, verseIndex.verse).toBlocking().first());
                        }
                        return new Pair<>(bookmarks, verses);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Pair<List<Bookmark>, List<Verse>>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        final BookmarksView v = getView();
                        if (v != null) {
                            v.onBookmarksLoadFailed();
                        }
                    }

                    @Override
                    public void onNext(Pair<List<Bookmark>, List<Verse>> bookmarks) {
                        final BookmarksView v = getView();
                        if (v != null) {
                            v.onBookmarksLoaded(bookmarks.first, bookmarks.second);
                        }
                    }
                });
    }
}
