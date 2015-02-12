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

package net.zionsoft.obadiah.legacy;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import net.zionsoft.obadiah.Constants;

public class Upgrader {
    public static void upgrade(Context context) {
        final SharedPreferences preferences = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        final int version = preferences.getInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, 0);
        final int applicationVersion;
        try {
            //noinspection ConstantConditions
            applicationVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never reach here
            return;
        }
        if (version >= applicationVersion)
            return;

        final SharedPreferences.Editor editor = preferences.edit();
        if (version < 10500) {
            // TODO remove everything in getFilesDir()
            editor.remove("selectedTranslation")
                    .putInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, 10500);
        }
        if (version < 10700) {
            editor.remove("lastUpdated")
                    .putInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, 10700);
        }
        if (version < 10800) {
            editor.remove("PREF_KEY_DOWNLOADING_TRANSLATION").remove("PREF_KEY_DOWNLOADING_TRANSLATION_LIST")
                    .remove("PREF_KEY_REMOVING_TRANSLATION").remove("PREF_KEY_UPGRADING")
                    .putInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, 10800);
        }
        if (version < 10802) {
            editor.remove("screenOn")
                    .putInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, 10802);
        }
        editor.putInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, applicationVersion)
                .apply();
    }
}
