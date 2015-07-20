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

package net.zionsoft.obadiah.model.utils;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.DateUtils;

import com.crashlytics.android.Crashlytics;

import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.model.network.NetworkHelper;

import org.json.JSONObject;

public class AppUpdateChecker extends IntentService {
    public static void check(Context context) {
        context.startService(new Intent(context, AppUpdateChecker.class));
    }

    public AppUpdateChecker() {
        super("net.zionsoft.obadiah.model.utils.AppUpdateChecker");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            // we only check if at least 24 hours is passed
            final SharedPreferences preferences
                    = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
            final long now = System.currentTimeMillis();
            final long lastCheckedTimestamp = preferences.getLong(
                    Constants.PREF_KEY_CHECKED_APPLICATION_VERSION_TIMESTAMP, 0);
            if (now - lastCheckedTimestamp < DateUtils.DAY_IN_MILLIS) {
                return;
            }

            // we only check if the user has active WiFi or WiMAX
            final NetworkInfo networkInfo
                    = ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected()) {
                return;
            }
            final int networkType = networkInfo.getType();
            if (networkType != ConnectivityManager.TYPE_WIFI
                    && networkType != ConnectivityManager.TYPE_WIMAX) {
                return;
            }

            final String response = new String(NetworkHelper.get(NetworkHelper.CLIENT_VERSION_URL), "UTF-8");
            final JSONObject versionObject = new JSONObject(response);
            final int latestVersion = versionObject.getInt("versionCode");
            final SharedPreferences.Editor editor = preferences.edit();
            if (latestVersion < preferences.getInt(Constants.PREF_KEY_CHECKED_APPLICATION_VERSION, 0)) {
                editor.putInt(Constants.PREF_KEY_CHECKED_APPLICATION_VERSION, latestVersion)
                        .putBoolean(Constants.PREF_KEY_ASKED_APPLICATION_UPDATE, false);
            }

            editor.putLong(Constants.PREF_KEY_CHECKED_APPLICATION_VERSION_TIMESTAMP, now).apply();
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
        }
    }

    public static boolean shouldUpdate(Context context) {
        try {
            final SharedPreferences preferences
                    = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
            if (preferences.getBoolean(Constants.PREF_KEY_ASKED_APPLICATION_UPDATE, false)) {
                return false;
            }

            final int currentVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            final int availableVersion = preferences.getInt(Constants.PREF_KEY_CHECKED_APPLICATION_VERSION, 0);
            return currentVersion < availableVersion;
        } catch (PackageManager.NameNotFoundException e) {
            Crashlytics.getInstance().core.logException(e);
            return false;
        }
    }

    public static void markAsUpdateAsked(Context context) {
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(Constants.PREF_KEY_ASKED_APPLICATION_UPDATE, false)
                .apply();
    }
}
