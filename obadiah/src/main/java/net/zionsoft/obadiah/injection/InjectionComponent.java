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
import net.zionsoft.obadiah.BookSelectionActivity;
import net.zionsoft.obadiah.injection.components.OpenSourceLicenseComponent;
import net.zionsoft.obadiah.injection.components.ReadingProgressComponent;
import net.zionsoft.obadiah.injection.components.SearchComponent;
import net.zionsoft.obadiah.injection.components.TranslationManagementComponent;
import net.zionsoft.obadiah.injection.modules.OpenSourceLicenseModule;
import net.zionsoft.obadiah.injection.modules.ReadingProgressModule;
import net.zionsoft.obadiah.injection.modules.SearchModule;
import net.zionsoft.obadiah.injection.modules.TranslationManagementModule;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.ReadingProgressManager;
import net.zionsoft.obadiah.model.notification.PushNotificationHandler;
import net.zionsoft.obadiah.model.translations.TranslationManager;
import net.zionsoft.obadiah.ui.activities.SettingsActivity;
import net.zionsoft.obadiah.ui.activities.TranslationManagementActivity;
import net.zionsoft.obadiah.ui.adapters.TranslationListAdapter;
import net.zionsoft.obadiah.ui.adapters.VerseListAdapter;
import net.zionsoft.obadiah.ui.adapters.VersePagerAdapter;
import net.zionsoft.obadiah.ui.fragments.ChapterSelectionFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = BaseInjectionModule.class)
public interface InjectionComponent {
    public void inject(App app);

    public void inject(BookSelectionActivity bookSelectionActivity);

    public void inject(SettingsActivity settingsActivity);

    public void inject(TranslationManagementActivity translationManagementActivity);

    public void inject(ChapterSelectionFragment chapterSelectionFragment);

    public void inject(TranslationListAdapter translationListAdapter);

    public void inject(VerseListAdapter verseListAdapter);

    public void inject(VersePagerAdapter versePagerAdapter);

    public void inject(PushNotificationHandler pushNotificationHandler);

    public void inject(Bible bible);

    public void inject(ReadingProgressManager readingProgressManager);

    public void inject(TranslationManager translationManager);

    OpenSourceLicenseComponent plus(OpenSourceLicenseModule openSourceLicenseModule);

    ReadingProgressComponent plus(ReadingProgressModule readingProgressModule);

    SearchComponent plus(SearchModule searchModule);

    TranslationManagementComponent plus(TranslationManagementModule translationManagementModule);

    final class Initializer {
        public static InjectionComponent init(App app) {
            return DaggerInjectionComponent.builder()
                    .baseInjectionModule(new InjectionModule(app))
                    .build();
        }
    }
}
