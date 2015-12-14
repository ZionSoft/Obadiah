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

package net.zionsoft.obadiah.readingprogress;

import android.util.Pair;

import net.zionsoft.obadiah.model.domain.ReadingProgress;
import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.ReadingProgressModel;
import net.zionsoft.obadiah.mvp.MVPPresenter;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

class ReadingProgressPresenter extends MVPPresenter<ReadingProgressView> {
    private final BibleReadingModel bibleReadingModel;
    private final ReadingProgressModel readingProgressModel;

    private CompositeSubscription subscription;

    ReadingProgressPresenter(BibleReadingModel bibleReadingModel,
                             ReadingProgressModel readingProgressModel) {
        this.bibleReadingModel = bibleReadingModel;
        this.readingProgressModel = readingProgressModel;
    }

    @Override
    protected void onViewTaken() {
        super.onViewTaken();
        subscription = new CompositeSubscription();
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }

    void loadReadingProgress() {
        subscription.add(Observable.zip(readingProgressModel.loadReadingProgress(),
                bibleReadingModel.loadBookNames(bibleReadingModel.loadCurrentTranslation()),
                new Func2<ReadingProgress, List<String>, Pair<ReadingProgress, List<String>>>() {
                    @Override
                    public Pair<ReadingProgress, List<String>> call(ReadingProgress readingProgress, List<String> bookNames) {
                        return new Pair<>(readingProgress, bookNames);
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Pair<ReadingProgress, List<String>>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        ReadingProgressView v = getView();
                        if (v != null) {
                            v.onReadingProgressLoadFailed();
                        }
                    }

                    @Override
                    public void onNext(Pair<ReadingProgress, List<String>> readingProgress) {
                        ReadingProgressView v = getView();
                        if (v != null) {
                            v.onReadingProgressLoaded(readingProgress.first, readingProgress.second);
                        }
                    }
                }));
    }
}
