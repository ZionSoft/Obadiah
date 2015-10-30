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

import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.ReadingProgress;
import net.zionsoft.obadiah.model.ReadingProgressManager;

import java.util.List;

import rx.Observable;
import rx.Subscriber;

public class ReadingProgressModel {
    private final Bible bible;
    private final ReadingProgressManager readingProgressManager;

    public ReadingProgressModel(Bible bible, ReadingProgressManager readingProgressManager) {
        this.bible = bible;
        this.readingProgressManager = readingProgressManager;
    }

    public Observable<List<String>> loadBookNames(final String translation) {
        return Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(Subscriber<? super List<String>> subscriber) {
                subscriber.onNext(bible.loadBookNames(translation));
                subscriber.onCompleted();
            }
        });
    }

    public Observable<ReadingProgress> loadReadingProgress() {
        return Observable.create(new Observable.OnSubscribe<ReadingProgress>() {
            @Override
            public void call(Subscriber<? super ReadingProgress> subscriber) {
                subscriber.onNext(readingProgressManager.loadReadingProgress());
                subscriber.onCompleted();
            }
        });
    }
}
