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

package net.zionsoft.obadiah.mvp.presenters;

import net.zionsoft.obadiah.model.ReadingProgress;
import net.zionsoft.obadiah.mvp.models.ReadingProgressModel;
import net.zionsoft.obadiah.mvp.views.ReadingProgressView;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class ReadingProgressPresenter extends MVPPresenter<ReadingProgressView> {
    private final ReadingProgressModel readingProgressModel;

    private CompositeSubscription subscription;

    public ReadingProgressPresenter(ReadingProgressModel readingProgressModel) {
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

    public void loadBookNames(String translation) {
        subscription.add(readingProgressModel.loadBookNames(translation)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<String>>() {
                    @Override
                    public void call(List<String> bookNames) {
                        ReadingProgressView v = getView();
                        if (v != null) {
                            if (bookNames != null && bookNames.size() > 0) {
                                v.onBookNamesLoaded(bookNames);
                            } else {
                                v.onBookNamesLoadFailed();
                            }
                        }
                    }
                }));
    }

    public void loadReadingProgress() {
        subscription.add(readingProgressModel.loadReadingProgress()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ReadingProgress>() {
                    @Override
                    public void call(ReadingProgress readingProgress) {
                        ReadingProgressView v = getView();
                        if (v != null) {
                            if (readingProgress != null) {
                                v.onReadingProgressLoaded(readingProgress);
                            } else {
                                v.onReadingProgressLoadFailed();
                            }
                        }
                    }
                }));
    }
}
