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

package net.zionsoft.obadiah.injection;

import android.content.Context;

import com.squareup.moshi.Moshi;
import com.squareup.okhttp.OkHttpClient;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.BookmarkModel;
import net.zionsoft.obadiah.model.datamodel.NoteModel;
import net.zionsoft.obadiah.model.datamodel.ReadingProgressModel;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.network.BackendInterface;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit.MoshiConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;

@Module
public class BaseInjectionModule {
    private final App application;

    public BaseInjectionModule(App application) {
        this.application = application;
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return application;
    }

    @Provides
    @Singleton
    public DatabaseHelper provideDatabaseHelper() {
        return new DatabaseHelper(application);
    }

    @Provides
    @Singleton
    public BibleReadingModel provideBibleReadingModel(Context context, DatabaseHelper databaseHelper) {
        return new BibleReadingModel(context, databaseHelper);
    }

    @Provides
    @Singleton
    public BookmarkModel provideBookmarkModel(DatabaseHelper databaseHelper) {
        return new BookmarkModel(databaseHelper);
    }

    @Provides
    @Singleton
    public NoteModel provideNoteModel(DatabaseHelper databaseHelper) {
        return new NoteModel(databaseHelper);
    }

    @Provides
    @Singleton
    public ReadingProgressModel provideReadingProgressModel(DatabaseHelper databaseHelper) {
        return new ReadingProgressModel(databaseHelper);
    }

    @Provides
    @Singleton
    public Settings provideSettings() {
        return new Settings(application);
    }

    @Provides
    @Singleton
    public Moshi provideMoshi() {
        return new Moshi.Builder().build();
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient() {
        final OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.setConnectTimeout(30L, TimeUnit.SECONDS);
        okHttpClient.setReadTimeout(30L, TimeUnit.SECONDS);
        okHttpClient.setWriteTimeout(30L, TimeUnit.SECONDS);
        return okHttpClient;
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(Moshi moshi, OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(BackendInterface.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
    }

    @Provides
    @Singleton
    public BackendInterface provideBackendInterface(Retrofit retrofit) {
        return retrofit.create(BackendInterface.class);
    }
}
