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

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.mvp.MVPPresenter;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class VersePresenter extends MVPPresenter<VerseView> {
    private final BibleReadingModel bibleReadingModel;
    private Subscription subscription;

    public VersePresenter(BibleReadingModel bibleReadingModel) {
        this.bibleReadingModel = bibleReadingModel;
    }

    @Override
    protected void onViewTaken() {
        super.onViewTaken();

        subscription = bibleReadingModel.observeCurrentReadingProgress()
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
                        final VerseView v = getView();
                        if (v != null) {
                            v.onReadingProgressUpdated(index);
                        }
                    }
                });
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
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
