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

package net.zionsoft.obadiah.translations;

import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.okhttp.ResponseBody;

import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.BookNamesTableHelper;
import net.zionsoft.obadiah.model.database.TranslationHelper;
import net.zionsoft.obadiah.model.database.TranslationsTableHelper;
import net.zionsoft.obadiah.model.domain.TranslationInfo;
import net.zionsoft.obadiah.network.BackendChapter;
import net.zionsoft.obadiah.network.BackendInterface;
import net.zionsoft.obadiah.network.BackendTranslationInfo;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okio.BufferedSource;
import okio.Okio;
import retrofit.Response;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

class TranslationManagementModel {
    private final SQLiteDatabase database;
    private final BackendInterface backendInterface;
    private final JsonAdapter<BackendTranslationInfo> translationInfoJsonAdapter;
    private final JsonAdapter<BackendChapter> chapterJsonAdapter;

    TranslationManagementModel(SQLiteDatabase database, Moshi moshi, BackendInterface backendInterface) {
        this.database = database;
        this.backendInterface = backendInterface;
        this.translationInfoJsonAdapter = moshi.adapter(BackendTranslationInfo.class);
        this.chapterJsonAdapter = moshi.adapter(BackendChapter.class);
    }

    Observable<Translations> loadTranslations(boolean forceRefresh) {
        Observable<List<TranslationInfo>> translations;
        if (forceRefresh) {
            translations = loadFromNetwork();
        } else {
            translations = Observable.concat(loadFromLocal(), loadFromNetwork())
                    .first(new Func1<List<TranslationInfo>, Boolean>() {
                        @Override
                        public Boolean call(List<TranslationInfo> translations) {
                            return translations.size() > 0;
                        }
                    });
        }
        translations = translations.map(new Func1<List<TranslationInfo>, List<TranslationInfo>>() {
            @Override
            public List<TranslationInfo> call(List<TranslationInfo> translations) {
                return sortByLocale(translations);
            }
        });
        final Observable<List<String>> downloaded = Observable.create(
                new Observable.OnSubscribe<List<String>>() {
                    @Override
                    public void call(Subscriber<? super List<String>> subscriber) {
                        try {
                            subscriber.onNext(TranslationsTableHelper.getDownloadedTranslations(database));
                            subscriber.onCompleted();
                        } catch (Exception e) {
                            subscriber.onError(e);
                        }
                    }
                });
        return Observable.zip(translations, downloaded, new Func2<List<TranslationInfo>, List<String>, Translations>() {
            @Override
            public Translations call(List<TranslationInfo> translations, List<String> downloaded) {
                return new Translations.Builder()
                        .translations(translations)
                        .downloaded(downloaded)
                        .build();
            }
        });
    }

    private Observable<List<TranslationInfo>> loadFromNetwork() {
        return backendInterface.fetchTranslations()
                .doOnNext(new Action1<List<TranslationInfo>>() {
                    @Override
                    public void call(List<TranslationInfo> translations) {
                        try {
                            database.beginTransaction();
                            TranslationsTableHelper.saveTranslations(database, translations);
                            database.setTransactionSuccessful();
                        } finally {
                            if (database.inTransaction()) {
                                database.endTransaction();
                            }
                        }
                    }
                })
                        // workaround for Retrofit / okhttp issue (of sorts)
                        // https://github.com/square/retrofit/issues/1046
                        // https://github.com/square/okhttp/issues/1592
                .unsubscribeOn(Schedulers.io());
    }

    private Observable<List<TranslationInfo>> loadFromLocal() {
        return Observable.create(new Observable.OnSubscribe<List<TranslationInfo>>() {
            @Override
            public void call(Subscriber<? super List<TranslationInfo>> subscriber) {
                try {
                    subscriber.onNext(TranslationsTableHelper.getTranslations(database));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    private static List<TranslationInfo> sortByLocale(List<TranslationInfo> translations) {
        Collections.sort(translations, new Comparator<TranslationInfo>() {
            @Override
            public int compare(TranslationInfo translation1, TranslationInfo translation2) {
                // first compares with user's default locale
                final Locale userLocale = Locale.getDefault();
                final String userLanguage = userLocale.getLanguage().toLowerCase();
                final String userCountry = userLocale.getCountry().toLowerCase();
                final String[] fields1 = translation1.language.split("_");
                final String[] fields2 = translation2.language.split("_");
                final int score1 = compareLocale(fields1[0], fields1[1],
                        userLanguage, userCountry);
                final int score2 = compareLocale(fields2[0], fields2[1],
                        userLanguage, userCountry);
                int r = score2 - score1;
                if (r != 0) {
                    return r;
                }

                // then sorts by language & name
                r = translation1.language.compareTo(translation2.language);
                return r == 0 ? translation1.name.compareTo(translation2.name) : r;
            }
        });
        return translations;
    }

    private static int compareLocale(String language, String country, String targetLanguage, String targetCountry) {
        if (language.equals(targetLanguage)) {
            return (country.equals(targetCountry)) ? 2 : 1;
        }
        return 0;
    }

    Observable<Void> removeTranslation(final TranslationInfo translation) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    try {
                        database.beginTransaction();
                        TranslationHelper.removeTranslation(database, translation.shortName);
                        BookNamesTableHelper.removeBookNames(database, translation.shortName);
                        database.setTransactionSuccessful();
                    } finally {
                        if (database.inTransaction()) {
                            database.endTransaction();
                        }
                    }
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    Observable<Integer> fetchTranslation(final TranslationInfo translation) {
        return Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                final long timestamp = SystemClock.elapsedRealtime();
                boolean success = false;

                ZipInputStream is = null;
                try {
                    final Response<ResponseBody> response
                            = backendInterface.fetchTranslation(translation.blobKey).execute();
                    if (response.code() != 200) {
                        throw new IOException("Unsupported HTTP status code - " + response.code());
                    }

                    database.beginTransaction();
                    TranslationHelper.createTranslationTable(database, translation.shortName);

                    is = new ZipInputStream(response.body().byteStream());
                    ZipEntry entry;
                    int downloaded = 0;
                    int progress = -1;
                    while ((entry = is.getNextEntry()) != null) {
                        final String entryName = entry.getName();
                        BufferedSource bufferedSource = Okio.buffer(Okio.source(is));
                        if (entryName.equals("books.json")) {
                            BookNamesTableHelper.saveBookNames(database,
                                    translationInfoJsonAdapter.fromJson(bufferedSource));
                        } else {
                            final String[] parts = entryName.substring(0, entryName.length() - 5).split("-");
                            final int book = Integer.parseInt(parts[0]);
                            final int chapter = Integer.parseInt(parts[1]);
                            TranslationHelper.saveVerses(database, translation.shortName, book, chapter,
                                    chapterJsonAdapter.fromJson(bufferedSource).verses);
                        }

                        // only emits if the progress is actually changed
                        // should I move this logic to a filter?
                        final int currentProgress = ++downloaded / 12;
                        if (currentProgress > progress) {
                            progress = currentProgress;
                            // the progress should be less or equal than 100
                            // in this case, with Integer.valueOf() no small object will be created
                            //noinspection UnnecessaryBoxing
                            subscriber.onNext(Integer.valueOf(progress));
                        }
                    }
                    database.setTransactionSuccessful();
                    subscriber.onCompleted();
                    success = true;
                } catch (Exception e) {
                    subscriber.onError(e);
                } finally {
                    Analytics.trackTranslationDownload(translation.shortName, success, SystemClock.elapsedRealtime() - timestamp);

                    if (database.inTransaction()) {
                        database.endTransaction();
                    }

                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            // not much we can do here
                        }
                    }
                }
            }
        });
    }
}
