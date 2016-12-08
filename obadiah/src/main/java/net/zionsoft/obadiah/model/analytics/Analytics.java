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

package net.zionsoft.obadiah.model.analytics;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.analytics.FirebaseAnalytics;

public class Analytics {
    public static final String EVENT_LOGIN = FirebaseAnalytics.Event.LOGIN;
    public static final String EVENT_SELECT_CONTENT = FirebaseAnalytics.Event.SELECT_CONTENT;
    public static final String EVENT_SHARE = FirebaseAnalytics.Event.SHARE;
    public static final String EVENT_DOWNLOAD_FIRST_TRANSLATION = "download_first_translation";
    public static final String EVENT_DOWNLOAD_TRANSLATION = "download_translation";
    public static final String EVENT_REMOVE_TRANSLATION = "remove_translation";

    public static final String PARAM_CONTENT_TYPE = FirebaseAnalytics.Param.CONTENT_TYPE;
    public static final String PARAM_ITEM_ID = FirebaseAnalytics.Param.ITEM_ID;
    public static final String PARAM_ELAPSED_TIME = "elapsed_time";

    private static FirebaseAnalytics analytics;

    public static void initialize(Context context) {
        if (analytics == null) {
            synchronized (Analytics.class) {
                if (analytics == null) {
                    analytics = FirebaseAnalytics.getInstance(context);
                }
            }
        }
    }

    public static void logEvent(@NonNull String event) {
        logEvent(event, null);
    }

    public static void logEvent(@NonNull String event, @Nullable Bundle params) {
        if (analytics != null) {
            analytics.logEvent(event, params);
        }
    }
}
