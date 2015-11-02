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

package net.zionsoft.obadiah.mvp.models;

import net.zionsoft.obadiah.model.translations.TranslationInfo;
import net.zionsoft.obadiah.model.translations.TranslationManager;
import net.zionsoft.obadiah.model.translations.Translations;
import net.zionsoft.obadiah.network.BackendInterface;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

public class TranslationManagementModel {
    private final TranslationManager translationManager;
    private final BackendInterface backendInterface;

    public TranslationManagementModel(TranslationManager translationManager, BackendInterface backendInterface) {
        this.translationManager = translationManager;
        this.backendInterface = backendInterface;
    }

    public Observable<Translations> loadTranslations(final boolean forceRefresh) {
        final Observable<List<TranslationInfo>> observable;
        if (forceRefresh) {
            observable = loadFromNetwork();
        } else {
            observable = Observable.concat(loadFromLocal(), loadFromNetwork())
                    .first(new Func1<List<TranslationInfo>, Boolean>() {
                        @Override
                        public Boolean call(List<TranslationInfo> translations) {
                            return translations.size() > 0;
                        }
                    });
        }
        return observable.map(new Func1<List<TranslationInfo>, Translations>() {
            @Override
            public Translations call(List<TranslationInfo> translations) {
                return new Translations.Builder()
                        .translations(translations)
                        .downloaded(translationManager.loadDownloadedTranslations())
                        .build();
            }
        });
    }

    private Observable<List<TranslationInfo>> loadFromNetwork() {
        return backendInterface.fetchTranslations()
                .doOnNext(new Action1<List<TranslationInfo>>() {
                    @Override
                    public void call(List<TranslationInfo> translations) {
                        translationManager.saveTranslations(translations);
                    }
                });
    }

    private Observable<List<TranslationInfo>> loadFromLocal() {
        return Observable.create(new Observable.OnSubscribe<List<TranslationInfo>>() {
            @Override
            public void call(Subscriber<? super List<TranslationInfo>> subscriber) {
                subscriber.onNext(translationManager.loadTranslations());
                subscriber.onCompleted();
            }
        });
    }

    public Observable<Boolean> removeTranslation(final TranslationInfo translation) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                subscriber.onNext(translationManager.removeTranslation(translation));
                subscriber.onCompleted();
            }
        });
    }
}
