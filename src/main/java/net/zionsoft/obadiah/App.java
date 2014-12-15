/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2014 ZionSoft
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

import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;

import net.zionsoft.obadiah.injection.Dagger_InjectionComponent;
import net.zionsoft.obadiah.injection.InjectionComponent;
import net.zionsoft.obadiah.injection.InjectionModule;
import net.zionsoft.obadiah.legacy.Upgrader;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.notification.PushNotificationRegister;
import net.zionsoft.obadiah.ui.utils.UiHelper;

import javax.inject.Inject;

public class App extends Application {
    @Inject
    Bible mBible;

    private InjectionComponent mInjectionComponent;

    public static App get(Context context) {
        return (App) context.getApplicationContext();
    }

    public InjectionComponent getInjectionComponent() {
        return mInjectionComponent;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG) {
            Crashlytics.start(this);
        }

        mInjectionComponent = Dagger_InjectionComponent.builder()
                .injectionModule(new InjectionModule(this))
                .build();
        mInjectionComponent.inject(this);

        Analytics.initialize(this);
        PushNotificationRegister.register(this);

        Upgrader.upgrade(this);

        UiHelper.forceActionBarOverflowMenu(this);
    }

    @Override
    public void onLowMemory() {
        mBible.clearCache();

        super.onLowMemory();
    }
}
