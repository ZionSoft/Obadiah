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

import rx.Observable;
import rx.Subscriber;

public class TranslationManagementModel {
    private final TranslationManager translationManager;

    public TranslationManagementModel(TranslationManager translationManager) {
        this.translationManager = translationManager;
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
