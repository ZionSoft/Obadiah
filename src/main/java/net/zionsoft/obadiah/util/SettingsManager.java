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

    public void refresh() {
        // night / day mode
        if (mSharedPreferences.getBoolean(SETTING_KEY_NIGHTMODE, false)) {
            mBackgroundColor = Color.BLACK;
            mTextColor = Color.WHITE;
        } else {
            mBackgroundColor = Color.WHITE;
            mTextColor = Color.BLACK;
        }
    }

    public int backgroundColor() {
        return mBackgroundColor;
    }

    public int textColor() {
        return mTextColor;
    }

    private static final String SETTING_KEY_NIGHTMODE = "pref_nightmode";

    private final SharedPreferences mSharedPreferences;

    private int mBackgroundColor;
    private int mTextColor;
}
