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

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.BookmarkModel;
import net.zionsoft.obadiah.model.datamodel.NoteModel;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.Note;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.mvp.BasePresenter;
import net.zionsoft.obadiah.utils.RxHelper;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Func3;
import rx.subscriptions.CompositeSubscription;

public class VersePagerPresenter extends BasePresenter<VersePagerView> {
    private final BibleReadingModel bibleReadingModel;
    private final BookmarkModel bookmarkModel;
    private final NoteModel noteModel;
    private CompositeSubscription subscription;

    public VersePagerPresenter(BibleReadingModel bibleReadingModel, BookmarkModel bookmarkModel,
                               NoteModel noteModel, Settings settings) {
        super(settings);
        this.bibleReadingModel = bibleReadingModel;
        this.bookmarkModel = bookmarkModel;
        this.noteModel = noteModel;
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
        bibleReadingModel.saveReadingProgress(VerseIndex.create(book, chapter, verse));
    }

    void loadVerses(final int book, final int chapter) {
        final Observable<List<Verse>> loadVerseObservable;
        if (bibleReadingModel.hasParallelTranslation()) {
            loadVerseObservable = bibleReadingModel.loadVersesWithParallelTranslations(book, chapter);
        } else {
            loadVerseObservable = bibleReadingModel.loadVerses(loadCurrentTranslation(), book, chapter);
        }

        final Observable<VerseList> observable;
        if (getSettings().isSimpleReading()) {
            observable = loadVerseObservable.map(new Func1<List<Verse>, VerseList>() {
                @Override
                public VerseList call(List<Verse> verses) {
                    return new VerseList(verses, null, null);
                }
            });
        } else {
            observable = Observable.zip(loadVerseObservable,
                    bookmarkModel.loadBookmarks(book, chapter), noteModel.loadNotes(book, chapter),
                    new Func3<List<Verse>, List<Bookmark>, List<Note>, VerseList>() {
                        @Override
                        public VerseList call(List<Verse> verses, List<Bookmark> bookmarks, List<Note> notes) {
                            return new VerseList(verses, bookmarks, notes);
                        }
                    });
        }

        getSubscription().add(observable.compose(RxHelper.<VerseList>applySchedulers())
                .subscribe(new Subscriber<VerseList>() {
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
                    public void onNext(VerseList result) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onVersesLoaded(result.verses, result.bookmarks, result.notes);
                        }
                    }
                }));
    }

    void addBookmark(final VerseIndex verseIndex) {
        getSubscription().add(bookmarkModel.addBookmark(verseIndex)
                .compose(RxHelper.<Bookmark>applySchedulers())
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
                .compose(RxHelper.<Void>applySchedulers())
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

    void updateNote(final VerseIndex verseIndex, final String note) {
        getSubscription().add(noteModel.updateNote(verseIndex, note)
                .compose(RxHelper.<Note>applySchedulers())
                .subscribe(new Subscriber<Note>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onNoteUpdateFailed(verseIndex, note);
                        }
                    }

                    @Override
                    public void onNext(Note note) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onNoteUpdated(note);
                        }
                    }
                }));
    }

    void removeNote(final VerseIndex verseIndex) {
        getSubscription().add(noteModel.removeNote(verseIndex)
                .compose(RxHelper.<Void>applySchedulers())
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onNoteRemoved(verseIndex);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onNoteRemoveFailed(verseIndex);
                        }
                    }

                    @Override
                    public void onNext(Void v) {
                        // should not reach here
                    }
                }));
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
