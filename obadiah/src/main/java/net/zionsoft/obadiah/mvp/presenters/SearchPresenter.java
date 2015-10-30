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

import net.zionsoft.obadiah.model.Verse;
import net.zionsoft.obadiah.mvp.models.SearchModel;
import net.zionsoft.obadiah.mvp.views.SearchView;

import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class SearchPresenter extends MVPPresenter<SearchView> {
    private final SearchModel searchModel;

    private Subscription subscription;

    public SearchPresenter(SearchModel searchModel) {
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

    public void search(String translation, String query) {
        subscription = searchModel.search(translation, query)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Verse>>() {
                    @Override
                    public void call(List<Verse> verses) {
                        SearchView v = getView();
                        if (v != null) {
                            if (verses != null) {
                                v.onVersesSearched(verses);
                            } else {
                                v.onVersesSearchFailed();
                            }
                        }
                    }
                });
    }

    public void clearSearchHistory() {
        searchModel.clearSearchHistory()
                .subscribeOn(Schedulers.io())
                .subscribe();
    }
}
