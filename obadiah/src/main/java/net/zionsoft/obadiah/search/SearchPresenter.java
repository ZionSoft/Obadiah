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

package net.zionsoft.obadiah.search;

import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.mvp.MVPPresenter;

import java.util.List;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

class SearchPresenter extends MVPPresenter<SearchView> {
    private final SearchModel searchModel;

    private Subscription subscription;

    SearchPresenter(SearchModel searchModel) {
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
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Verse>>() {
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
                    public void onNext(List<Verse> verses) {
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
}
