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

import android.content.Context;
import android.provider.SearchRecentSuggestions;

import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.domain.Verse;

import java.util.List;

import rx.Observable;
import rx.Subscriber;

class SearchModel {
    private final Bible bible;
    private final SearchRecentSuggestions recentSearches;

    SearchModel(Context context, Bible bible) {
        this.bible = bible;
        this.recentSearches = new SearchRecentSuggestions(context,
                RecentSearchProvider.AUTHORITY, RecentSearchProvider.MODE);
    }

    Observable<List<Verse>> search(final String translation, final String query) {
        recentSearches.saveRecentQuery(query, null);
        return Observable.create(new Observable.OnSubscribe<List<Verse>>() {
            @Override
            public void call(Subscriber<? super List<Verse>> subscriber) {
                try {
                    subscriber.onNext(bible.search(translation, query));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    Observable<Void> clearSearchHistory() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                recentSearches.clearHistory();
                subscriber.onCompleted();
            }
        });
    }
}
