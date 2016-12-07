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

package net.zionsoft.obadiah;

import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.SyncModel;
import net.zionsoft.obadiah.notification.PushNotificationRegister;
import net.zionsoft.obadiah.ui.utils.UiHelper;

import javax.inject.Inject;

import dagger.Lazy;

public class App extends BaseApp {
    @Inject
    Lazy<BibleReadingModel> bibleReadingModel;

    @Inject
    SyncModel syncModel;

    private static AppComponent appComponent;

    public static AppComponent getComponent() {
        return appComponent;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        appComponent = AppComponent.Initializer.init(this);
        appComponent.inject(this);

        Analytics.initialize(this);
        PushNotificationRegister.register(this);

        UiHelper.forceActionBarOverflowMenu(this);
    }

    @Override
    public void onLowMemory() {
        bibleReadingModel.get().clearCache();

        super.onLowMemory();
    }
}
