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

package net.zionsoft.obadiah.translations;

import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;

import com.crashlytics.android.Crashlytics;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonEncodingException;
import com.squareup.moshi.Moshi;

import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.BookNamesTableHelper;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.database.TranslationHelper;
import net.zionsoft.obadiah.model.database.TranslationsTableHelper;
import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.domain.TranslationInfo;
import net.zionsoft.obadiah.network.BackendBooks;
import net.zionsoft.obadiah.network.BackendChapter;
import net.zionsoft.obadiah.network.BackendInterface;
import net.zionsoft.obadiah.network.BackendTranslationInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;
import retrofit2.Response;
import rx.Emitter;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

class TranslationManagementModel {
    final DatabaseHelper databaseHelper;
    final BibleReadingModel bibleReadingModel;
    final BackendInterface backendInterface;
    final JsonAdapter<BackendBooks> translationInfoJsonAdapter;
    final JsonAdapter<BackendChapter> chapterJsonAdapter;

    TranslationManagementModel(DatabaseHelper databaseHelper, BibleReadingModel bibleReadingModel,
                               Moshi moshi, BackendInterface backendInterface) {
        this.databaseHelper = databaseHelper;
        this.bibleReadingModel = bibleReadingModel;
        this.backendInterface = backendInterface;
        this.translationInfoJsonAdapter = moshi.adapter(BackendBooks.class);
        this.chapterJsonAdapter = moshi.adapter(BackendChapter.class).lenient(); // we have some characters to escape illegally
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
        final Observable<List<String>> downloaded = Observable.fromCallable(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return TranslationsTableHelper.getDownloadedTranslations(databaseHelper.getDatabase());
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
        final long timestamp = SystemClock.elapsedRealtime();
        return backendInterface.fetchTranslations()
                .map(new Func1<List<BackendTranslationInfo>, List<TranslationInfo>>() {
                    @Override
                    public List<TranslationInfo> call(List<BackendTranslationInfo> backendTranslations) {
                        final int count = backendTranslations.size();
                        final List<TranslationInfo> translations = new ArrayList<>(count);
                        for (int i = 0; i < count; ++i) {
                            translations.add(backendTranslations.get(i).toTranslationInfo());
                        }
                        return translations;
                    }
                })
                .doOnNext(new Action1<List<TranslationInfo>>() {
                    @Override
                    public void call(List<TranslationInfo> translations) {
                        final SQLiteDatabase database = databaseHelper.getDatabase();
                        try {
                            database.beginTransaction();
                            TranslationsTableHelper.removeAllTranslations(database);
                            TranslationsTableHelper.saveTranslations(database, translations);
                            database.setTransactionSuccessful();
                        } finally {
                            if (database.inTransaction()) {
                                database.endTransaction();
                            }
                        }

                        Analytics.trackEvent(Analytics.CATEGORY_TRANSLATION,
                                Analytics.TRANSLATION_ACTION_LIST_DOWNLOADED, null,
                                SystemClock.elapsedRealtime() - timestamp);
                    }
                })
                // workaround for Retrofit / okhttp issue (of sorts)
                // https://github.com/square/retrofit/issues/1046
                // https://github.com/square/okhttp/issues/1592
                .unsubscribeOn(Schedulers.io());
    }

    private Observable<List<TranslationInfo>> loadFromLocal() {
        return Observable.fromCallable(new Callable<List<TranslationInfo>>() {
            @Override
            public List<TranslationInfo> call() throws Exception {
                return TranslationsTableHelper.getTranslations(databaseHelper.getDatabase());
            }
        });
    }

    static List<TranslationInfo> sortByLocale(List<TranslationInfo> translations) {
        Collections.sort(translations, new Comparator<TranslationInfo>() {
            @Override
            public int compare(TranslationInfo translation1, TranslationInfo translation2) {
                // first compares with user's default locale
                final String userLanguage = Locale.getDefault().getLanguage().toLowerCase(Locale.getDefault());
                final String targetLanguage1 = translation1.language().split("_")[0];
                final String targetLanguage2 = translation2.language().split("_")[0];
                final int score1 = userLanguage.equals(targetLanguage1) ? 1 : 0;
                final int score2 = userLanguage.equals(targetLanguage2) ? 1 : 0;
                int r = score2 - score1;
                if (r != 0) {
                    return r;
                }

                // then sorts by language & name
                r = translation1.language().compareTo(translation2.language());
                return r == 0 ? translation1.name().compareTo(translation2.name()) : r;
            }
        });
        return translations;
    }

    Observable<Void> removeTranslation(final TranslationInfo translation) {
        return Observable.defer(new Func0<Observable<Void>>() {
            @Override
            public Observable<Void> call() {
                try {
                    final SQLiteDatabase database = databaseHelper.getDatabase();
                    try {
                        database.beginTransaction();
                        TranslationHelper.removeTranslation(database, translation.shortName());
                        BookNamesTableHelper.removeBookNames(database, translation.shortName());
                        bibleReadingModel.removeParallelTranslation(translation.shortName());
                        database.setTransactionSuccessful();
                    } finally {
                        if (database.inTransaction()) {
                            database.endTransaction();
                        }
                    }
                    Analytics.trackEvent(Analytics.CATEGORY_TRANSLATION,
                            Analytics.TRANSLATION_ACTION_REMOVED, translation.shortName());
                    return Observable.empty();
                } catch (Exception e) {
                    Crashlytics.getInstance().core.logException(e);
                    return Observable.error(e);
                }
            }
        });
    }

    Observable<Integer> fetchTranslation(final TranslationInfo translation) {
        return Observable.fromEmitter(new Action1<Emitter<Integer>>() {
            @Override
            public void call(Emitter<Integer> emitter) {
                final long timestamp = SystemClock.elapsedRealtime();
                final SQLiteDatabase database = databaseHelper.getDatabase();
                ZipInputStream is = null;
                try {
                    final Response<ResponseBody> response
                            = backendInterface.fetchTranslation(translation.blobKey()).execute();
                    if (response.code() != 200) {
                        throw new IOException("Unsupported HTTP status code - " + response.code());
                    }

                    database.beginTransaction();
                    TranslationHelper.createTranslationTable(database, translation.shortName());

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
                            TranslationHelper.saveVerses(database, translation.shortName(),
                                    book, chapter, chapterJsonAdapter.fromJson(bufferedSource).verses);
                        }

                        // only emits if the progress is actually changed
                        // should I move this logic to a filter?
                        final int currentProgress = ++downloaded / 12;
                        if (currentProgress > progress) {
                            progress = currentProgress;
                            // the progress should be less or equal than 100
                            // in this case, with Integer.valueOf() no small object will be created
                            //noinspection UnnecessaryBoxing
                            emitter.onNext(Integer.valueOf(progress));
                        }
                    }
                    database.setTransactionSuccessful();

                    Analytics.trackEvent(Analytics.CATEGORY_TRANSLATION,
                            Analytics.TRANSLATION_ACTION_DOWNLOADED, translation.shortName(),
                            SystemClock.elapsedRealtime() - timestamp);
                    emitter.onCompleted();
                } catch (Exception e) {
                    if (e instanceof JsonEncodingException) {
                        Analytics.trackEvent(Analytics.CATEGORY_TRANSLATION,
                                Analytics.TRANSLATION_ACTION_DOWNLOAD_FAILED, translation.shortName());
                    }
                    Crashlytics.getInstance().core.log("Failed to download translation: " + translation.shortName());
                    Crashlytics.getInstance().core.logException(e);
                    emitter.onError(e);
                } finally {
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
        }, Emitter.BackpressureMode.LATEST);
    }
}
