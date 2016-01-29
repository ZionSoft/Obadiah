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

package net.zionsoft.obadiah.biblereading.verse;

import android.support.annotation.NonNull;
import android.util.Pair;

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.BookmarkModel;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.mvp.BasePresenter;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class VersePagerPresenter extends BasePresenter<VersePagerView> {
    private final BibleReadingModel bibleReadingModel;
    private final BookmarkModel bookmarkModel;
    private CompositeSubscription subscription;

    public VersePagerPresenter(BibleReadingModel bibleReadingModel, BookmarkModel bookmarkModel, Settings settings) {
        super(settings);
        this.bibleReadingModel = bibleReadingModel;
        this.bookmarkModel = bookmarkModel;
    }

    @Override
    protected void onViewTaken() {
        super.onViewTaken();

        getSubscription().add(Observable.merge(
                bibleReadingModel.observeCurrentTranslation()
                        .map(new Func1<String, Void>() {
                            @Override
                            public Void call(String s) {
                                return null;
                            }
                        }),
                bibleReadingModel.observeParallelTranslation())
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(Void param) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onTranslationUpdated();
                        }
                    }
                }));
    }

    @NonNull
    private CompositeSubscription getSubscription() {
        if (subscription == null) {
            subscription = new CompositeSubscription();
        }
        return subscription;
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }

    String loadCurrentTranslation() {
        return bibleReadingModel.loadCurrentTranslation();
    }

    int loadCurrentBook() {
        return bibleReadingModel.loadCurrentBook();
    }

    int loadCurrentChapter() {
        return bibleReadingModel.loadCurrentChapter();
    }

    int loadCurrentVerse() {
        return bibleReadingModel.loadCurrentVerse();
    }

    void saveReadingProgress(int book, int chapter, int verse) {
        bibleReadingModel.saveReadingProgress(new VerseIndex(book, chapter, verse));
    }

    void loadVerses(final int book, final int chapter) {
        final Observable<List<Verse>> loadVerseObservable;
        if (bibleReadingModel.hasParallelTranslation()) {
            loadVerseObservable = bibleReadingModel.loadVersesWithParallelTranslations(book, chapter);
        } else {
            loadVerseObservable = bibleReadingModel.loadVerses(loadCurrentTranslation(), book, chapter);
        }
        getSubscription().add(loadVerseObservable.zipWith(bookmarkModel.loadBookmarks(book, chapter),
                new Func2<List<Verse>, List<Bookmark>, Pair<List<Verse>, List<Bookmark>>>() {
                    @Override
                    public Pair<List<Verse>, List<Bookmark>> call(List<Verse> verses, List<Bookmark> bookmarks) {
                        return new Pair<>(verses, bookmarks);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Pair<List<Verse>, List<Bookmark>>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onVersesLoadFailed(book, chapter);
                        }
                    }

                    @Override
                    public void onNext(Pair<List<Verse>, List<Bookmark>> result) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onVersesLoaded(result.first, result.second);
                        }
                    }
                }));
    }

    void addBookmark(final VerseIndex verseIndex) {
        getSubscription().add(bookmarkModel.addBookmark(verseIndex)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Bookmark>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onBookmarkAddFailed(verseIndex);
                        }
                    }

                    @Override
                    public void onNext(Bookmark bookmark) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onBookmarkAdded(bookmark);
                        }
                    }
                }));
    }

    void removeBookmark(final VerseIndex verseIndex) {
        getSubscription().add(bookmarkModel.removeBookmark(verseIndex)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onBookmarkRemoved(verseIndex);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onBookmarkRemoveFailed(verseIndex);
                        }
                    }

                    @Override
                    public void onNext(Void v) {
                        // should not reach here
                    }
                }));
    }

    void addNote(final VerseIndex verseIndex) {
        // TODO
    }

    void showNote(VerseIndex verseIndex) {
        final VersePagerView v = getView();
        if (v != null) {
            v.showNote(verseIndex);
        }
    }

    void hideNote(VerseIndex verseIndex) {
        final VersePagerView v = getView();
        if (v != null) {
            v.hideNote(verseIndex);
        }
    }
}
