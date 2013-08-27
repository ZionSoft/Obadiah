/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2013 ZionSoft
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

package net.zionsoft.obadiah.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

public class SettingsManager {
    public SettingsManager(Context context) {
        super();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean refresh() {
        boolean nightMode = mSharedPreferences.getBoolean(SETTING_KEY_NIGHT_MODE, false);
        if (nightMode) {
            if (mMode == NIGHT_MODE)
                return false;
            mMode = NIGHT_MODE;
        } else {
            if (mMode == DAY_MODE)
                return false;
            mMode = DAY_MODE;
        }
        return true;
    }

    public int backgroundColor() {
        return mMode == NIGHT_MODE ? Color.BLACK : Color.WHITE;
    }

    public int textColor() {
        return mMode == NIGHT_MODE ? Color.WHITE : Color.BLACK;
    }

    private static final String SETTING_KEY_NIGHT_MODE = "pref_night_mode";

    private static final int UNKNOWN_MODE = 0;
    private static final int DAY_MODE = 1;
    private static final int NIGHT_MODE = 2;

    private final SharedPreferences mSharedPreferences;
    private int mMode = UNKNOWN_MODE;
}
