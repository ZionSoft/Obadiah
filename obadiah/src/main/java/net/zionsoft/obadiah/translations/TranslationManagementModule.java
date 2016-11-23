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

import android.content.Context;

import com.squareup.moshi.Moshi;

import net.zionsoft.obadiah.injection.scopes.ActivityScope;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.Settings;

import dagger.Module;
import dagger.Provides;
import retrofit2.Retrofit;

@Module
public class TranslationManagementModule {
    @Provides
    AdsModel provideAdsModel(Context context, Moshi moshi) {
        return new AdsModel(context, moshi);
    }

    @Provides
    TranslationService provideBackendInterface(Retrofit retrofit) {
        return retrofit.create(TranslationService.class);
    }

    @Provides
    TranslationManagementModel provideTranslationManagementModel(
            DatabaseHelper databaseHelper, BibleReadingModel bibleReadingModel,
            Moshi moshi, TranslationService translationService) {
        return new TranslationManagementModel(databaseHelper, bibleReadingModel, moshi, translationService);
    }

    @Provides
    @ActivityScope
    TranslationManagementPresenter provideTranslationManagementPresenter(
            AdsModel adsModel, BibleReadingModel bibleReadingModel,
            TranslationManagementModel translationManagementModel, Settings settings) {
        return new TranslationManagementPresenter(adsModel, bibleReadingModel, translationManagementModel, settings);
    }
}
