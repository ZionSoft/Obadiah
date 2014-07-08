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
import android.preference.PreferenceManager;

import net.zionsoft.obadiah.legacy.Upgrader;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.ReadingProgressManager;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.ui.utils.UIHelper;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Upgrader.upgrade(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        Analytics.initialize(this);
        Bible.initialize(this);
        ReadingProgressManager.initialize(this);
        Settings.initialize(this);

        UIHelper.forceActionBarOverflowMenu(this);
    }

    @Override
    public void onLowMemory() {
        Bible.getInstance().clearCache();

        super.onLowMemory();
    }
}
