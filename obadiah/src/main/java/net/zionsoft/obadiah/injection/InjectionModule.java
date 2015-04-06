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

package net.zionsoft.obadiah.injection;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.ReadingProgressManager;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.translations.TranslationManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public final class InjectionModule {
    private final App application;

    public InjectionModule(App application) {
        this.application = application;
    }

    @Provides
    @Singleton
    public App provideApplication() {
        return application;
    }

    @Provides
    @Singleton
    public Bible provideBible() {
        return new Bible(application);
    }

    @Provides
    @Singleton
    public DatabaseHelper provideDatabaseHelper() {
        return new DatabaseHelper(application);
    }

    @Provides
    @Singleton
    public ReadingProgressManager provideReadingProgressManager() {
        return new ReadingProgressManager(application);
    }

    @Provides
    @Singleton
    public Settings provideSettings() {
        return new Settings(application);
    }

    @Provides
    @Singleton
    public TranslationManager provideTranslationManager() {
        return new TranslationManager(application);
    }
}
