/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2017 ZionSoft
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

package net.zionsoft.obadiah.biblereading.toolbar;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.mvp.MVPPresenter;
import net.zionsoft.obadiah.utils.RxHelper;

import java.util.List;

import rx.SingleSubscriber;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

public class ToolbarPresenter extends MVPPresenter<ToolbarView> {
    private final BibleReadingModel bibleReadingModel;
    private CompositeSubscription subscription;

    public ToolbarPresenter(BibleReadingModel bibleReadingModel) {
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
                        final ToolbarView v = getView();
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

    void loadBookNames(String translation) {
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
                        final ToolbarView v = getView();
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

    @Nullable
    String loadCurrentTranslation() {
        return bibleReadingModel.loadCurrentTranslation();
    }

    void saveCurrentTranslation(String translation) {
        bibleReadingModel.saveCurrentTranslation(translation);
    }

    int loadCurrentBook() {
        return bibleReadingModel.loadCurrentBook();
    }

    int loadCurrentChapter() {
        return bibleReadingModel.loadCurrentChapter();
    }

    void loadTranslations() {
        getSubscription().add(bibleReadingModel.loadTranslations()
                .compose(RxHelper.<List<String>>applySchedulersForSingle())
                .subscribe(new SingleSubscriber<List<String>>() {
                    @Override
                    public void onSuccess(List<String> translations) {
                        final ToolbarView v = getView();
                        if (v != null) {
                            if (translations.size() > 0) {
                                v.onTranslationsLoaded(translations);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        // do nothing
                    }
                }));
    }

    boolean isParallelTranslation(String translation) {
        return bibleReadingModel.isParallelTranslation(translation);
    }

    void loadParallelTranslation(String translation) {
        bibleReadingModel.addParallelTranslation(translation);
    }

    void removeParallelTranslation(String translation) {
        bibleReadingModel.removeParallelTranslation(translation);
    }

    void loadBookNamesForCurrentTranslation() {
        loadBookNames(bibleReadingModel.loadCurrentTranslation());
    }
}
