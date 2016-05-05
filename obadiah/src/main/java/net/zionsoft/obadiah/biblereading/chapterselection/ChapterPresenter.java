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

package net.zionsoft.obadiah.biblereading.chapterselection;

import android.support.annotation.NonNull;

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.mvp.MVPPresenter;
import net.zionsoft.obadiah.utils.RxHelper;

import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class ChapterPresenter extends MVPPresenter<ChapterView> {
    private final BibleReadingModel bibleReadingModel;
    private CompositeSubscription subscription;

    public ChapterPresenter(BibleReadingModel bibleReadingModel) {
        this.bibleReadingModel = bibleReadingModel;
    }

    @Override
    protected void onViewTaken() {
        super.onViewTaken();

        getSubscription().add(bibleReadingModel.observeCurrentTranslation()
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(String translation) {
                        loadBookNames(translation);
                    }
                }));
        getSubscription().add(bibleReadingModel.observeCurrentReadingProgress()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<VerseIndex>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(VerseIndex index) {
                        final ChapterView v = getView();
                        if (v != null) {
                            v.onReadingProgressUpdated(index);
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

    private void loadBookNames(String translation) {
        getSubscription().add(bibleReadingModel.loadBookNames(translation)
                .compose(RxHelper.<List<String>>applySchedulers())
                .subscribe(new Subscriber<List<String>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(List<String> bookNames) {
                        final ChapterView v = getView();
                        if (v != null) {
                            // if the list is empty, it means the requested translation is not
                            // installed yet, do nothing
                            if (bookNames.size() > 0) {
                                v.onBookNamesLoaded(bookNames);
                            }
                        }
                    }
                }));
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }

    void loadBookNamesForCurrentTranslation() {
        loadBookNames(bibleReadingModel.loadCurrentTranslation());
    }

    int loadCurrentBook() {
        return bibleReadingModel.loadCurrentBook();
    }

    int loadCurrentChapter() {
        return bibleReadingModel.loadCurrentChapter();
    }

    void saveReadingProgress(int book, int chapter, int verse) {
        bibleReadingModel.saveReadingProgress(VerseIndex.create(book, chapter, verse));
    }
}
