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

package net.zionsoft.obadiah.utils;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class RxHelper {
    private static final Observable.Transformer SCHEDULERS_TRANSFORMER_FOR_OBSERVABLE
            = new Observable.Transformer<Object, Object>() {
        @Override
        public Observable<Object> call(Observable<Object> observable) {
            return observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        }
    };

    private static final Single.Transformer SCHEDULERS_TRANSFORMER_FOR_SINGLE
            = new Single.Transformer<Object, Object>() {
        @Override
        public Single<Object> call(Single<Object> single) {
            return single.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        }
    };

    private static final Completable.Transformer SCHEDULERS_TRANSFORMER_FOR_COMPLETABLE
            = new Completable.Transformer() {
        @Override
        public Completable call(Completable completable) {
            return completable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        }
    };

    // idea stolen from http://blog.danlew.net/2015/03/02/dont-break-the-chain/
    public static <T> Observable.Transformer<T, T> applySchedulers() {
        //noinspection unchecked
        return (Observable.Transformer<T, T>) SCHEDULERS_TRANSFORMER_FOR_OBSERVABLE;
    }

    public static <T> Single.Transformer<T, T> applySchedulersForSingle() {
        //noinspection unchecked
        return (Single.Transformer<T, T>) SCHEDULERS_TRANSFORMER_FOR_SINGLE;
    }

    public static Completable.Transformer applySchedulersForCompletable() {
        return SCHEDULERS_TRANSFORMER_FOR_COMPLETABLE;
    }
}
