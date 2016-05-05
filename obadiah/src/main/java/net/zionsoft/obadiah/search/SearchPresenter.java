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

package net.zionsoft.obadiah.search;

import android.support.annotation.Nullable;

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.model.domain.VerseSearchResult;
import net.zionsoft.obadiah.mvp.BasePresenter;
import net.zionsoft.obadiah.utils.RxHelper;

import java.util.List;

import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

class SearchPresenter extends BasePresenter<SearchView> {
    private final BibleReadingModel bibleReadingModel;
    private final SearchModel searchModel;

    private Subscription subscription;

    SearchPresenter(BibleReadingModel bibleReadingModel, SearchModel searchModel, Settings settings) {
        super(settings);
        this.bibleReadingModel = bibleReadingModel;
        this.searchModel = searchModel;
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }

    void search(String translation, String query) {
        subscription = searchModel.search(translation, query)
                .compose(RxHelper.<List<VerseSearchResult>>applySchedulers())
                .subscribe(new Subscriber<List<VerseSearchResult>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        SearchView v = getView();
                        if (v != null) {
                            v.onVersesSearchFailed();
                        }
                    }

                    @Override
                    public void onNext(List<VerseSearchResult> verses) {
                        SearchView v = getView();
                        if (v != null) {
                            v.onVersesSearched(verses);
                        }
                    }
                });
    }

    void clearSearchHistory() {
        searchModel.clearSearchHistory()
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    @Nullable
    String loadCurrentTranslation() {
        return bibleReadingModel.loadCurrentTranslation();
    }

    void saveReadingProgress(VerseIndex index) {
        bibleReadingModel.saveReadingProgress(index);
    }

    void openBibleReadingActivity() {
        SearchView v = getView();
        if (v != null) {
            v.openBibleReadingActivity();
        }
    }
}
