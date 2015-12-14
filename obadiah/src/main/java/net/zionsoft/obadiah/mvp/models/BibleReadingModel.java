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

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.text.TextUtils;

import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.TranslationHelper;
import net.zionsoft.obadiah.model.domain.TranslationInfo;
import net.zionsoft.obadiah.model.domain.Verse;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

@Singleton
public class BibleReadingModel {
    private final SQLiteDatabase database;
    private final SharedPreferences preferences;

    private final LruCache<String, List<String>> bookNameCache
            = new LruCache<String, List<String>>((int) (Runtime.getRuntime().maxMemory() / 16L)) {
        @Override
        protected int sizeOf(String key, List<String> texts) {
            // strings are UTF-16 encoded (with a length of one or two 16-bit code units)
            int length = 0;
            for (String text : texts)
                length += text.length() * 4;
            return length;
        }
    };
    private final LruCache<String, List<Verse>> verseCache
            = new LruCache<String, List<Verse>>((int) (Runtime.getRuntime().maxMemory() / 8L)) {
        @Override
        protected int sizeOf(String key, List<Verse> verses) {
            // each Verse contains 3 integers and 2 strings
            // strings are UTF-16 encoded (with a length of one or two 16-bit code units)
            int length = 0;
            for (Verse verse : verses)
                length += 12 + (verse.bookName.length() + verse.verseText.length()) * 4;
            return length;
        }
    };

    @Inject
    public BibleReadingModel(Context context, SQLiteDatabase database) {
        this.preferences = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        this.database = database;
    }

    @Nullable
    public String loadCurrentTranslation() {
        return preferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
    }

    public void saveCurrentTranslation(TranslationInfo translation) {
        preferences.edit().putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, translation.shortName).apply();
        Analytics.trackTranslationSelection(translation.shortName);
    }

    public boolean hasDownloadedTranslation() {
        return !TextUtils.isEmpty(loadCurrentTranslation());
    }

    public int loadCurrentBook() {
        return preferences.getInt(Constants.PREF_KEY_LAST_READ_BOOK, 0);
    }

    public int loadCurrentChapter() {
        return preferences.getInt(Constants.PREF_KEY_LAST_READ_CHAPTER, 0);
    }

    public int loadCurrentVerse() {
        return preferences.getInt(Constants.PREF_KEY_LAST_READ_VERSE, 0);
    }

    public void setCurrentTranslation(String translation) {
        preferences.edit().putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, translation).apply();
    }

    public void storeReadingProgress(int book, int chapter, int verse) {
        preferences.edit()
                .putInt(Constants.PREF_KEY_LAST_READ_BOOK, book)
                .putInt(Constants.PREF_KEY_LAST_READ_CHAPTER, chapter)
                .putInt(Constants.PREF_KEY_LAST_READ_VERSE, verse)
                .apply();
    }

    public Observable<List<String>> loadTranslations() {
        return Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(Subscriber<? super List<String>> subscriber) {
                try {
                    subscriber.onNext(TranslationHelper.getDownloadedTranslationShortNames(database));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<List<String>> loadBookNames(String translation) {
        return Observable.concat(loadBookNamesFromCache(translation),
                loadBookNamesFromDatabase(translation))
                .first(new Func1<List<String>, Boolean>() {
                    @Override
                    public Boolean call(List<String> bookNames) {
                        return bookNames != null && bookNames.size() > 0;
                    }
                });
    }

    private Observable<List<String>> loadBookNamesFromCache(final String translation) {
        return Observable.just(bookNameCache.get(translation));
    }

    private Observable<List<String>> loadBookNamesFromDatabase(final String translation) {
        return Observable.create(new Observable.OnSubscribe<List<String>>() {
            @Override
            public void call(Subscriber<? super List<String>> subscriber) {
                try {
                    subscriber.onNext(Collections.unmodifiableList(
                            TranslationHelper.getBookNames(database, translation)));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).doOnNext(new Action1<List<String>>() {
            @Override
            public void call(List<String> bookNames) {
                bookNameCache.put(translation, bookNames);
            }
        });
    }

    public Observable<List<Verse>> loadVerses(String translation, int book, int chapter) {
        return Observable.concat(loadVersesFromCache(translation, book, chapter),
                loadVersesFromDatabase(translation, book, chapter))
                .first(new Func1<List<Verse>, Boolean>() {
                    @Override
                    public Boolean call(List<Verse> verses) {
                        return verses != null && verses.size() > 0;
                    }
                });
    }

    private Observable<List<Verse>> loadVersesFromCache(String translation, int book, int chapter) {
        return Observable.just(verseCache.get(buildVersesCacheKey(translation, book, chapter)));
    }

    private static final StringBuilder STRING_BUILDER = new StringBuilder(32);

    private static String buildVersesCacheKey(String translation, int book, int chapter) {
        synchronized (STRING_BUILDER) {
            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(translation).append('-').append(book).append('-').append(chapter);
            return STRING_BUILDER.toString();
        }
    }

    private Observable<List<Verse>> loadVersesFromDatabase(
            final String translation, final int book, final int chapter) {
        return loadBookNames(translation)
                .map(new Func1<List<String>, List<Verse>>() {
                    @Override
                    public List<Verse> call(List<String> bookNames) {
                        return Collections.unmodifiableList(TranslationHelper.getVerses(
                                database, translation, bookNames.get(book), book, chapter));
                    }
                }).doOnNext(new Action1<List<Verse>>() {
                    @Override
                    public void call(List<Verse> verses) {
                        verseCache.put(buildVersesCacheKey(translation, book, chapter), verses);
                    }
                });
    }

    public Observable<List<Verse>> search(final String translation, final String query) {
        return loadBookNames(translation)
                .map(new Func1<List<String>, List<Verse>>() {
                    @Override
                    public List<Verse> call(List<String> bookNames) {
                        return TranslationHelper.searchVerses(database, translation, bookNames, query);
                    }
                });
    }

    public void clearCache() {
        bookNameCache.evictAll();
        verseCache.evictAll();
    }
}
