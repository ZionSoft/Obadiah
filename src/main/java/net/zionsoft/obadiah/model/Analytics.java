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

package net.zionsoft.obadiah.model;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import net.zionsoft.obadiah.R;

public class Analytics {
    private static Tracker sTracker;

    public static void initialize(Context context) {
        if (sTracker == null) {
            synchronized (Analytics.class) {
                if (sTracker == null)
                    sTracker = GoogleAnalytics.getInstance(context.getApplicationContext()).newTracker(R.xml.analytics);
            }
        }
    }

    public static void trackException(String description) {
        sTracker.send(new HitBuilders.ExceptionBuilder()
                .setDescription(description).setFatal(false)
                .build());
    }

    public static void trackScreen(String name) {
        sTracker.setScreenName(name);
        sTracker.send(new HitBuilders.AppViewBuilder().build());
    }

    public static void trackTranslationListDownloading(boolean isSuccessful, long elapsedTime) {
        sTracker.send(new HitBuilders.EventBuilder()
                .setCategory("translation").setAction("download_list").setValue(isSuccessful ? elapsedTime : -1)
                .build());
    }

    public static void trackTranslationDownload(String translation, boolean isSuccessful, long elapsedTime) {
        sTracker.send(new HitBuilders.EventBuilder()
                .setCategory("translation").setAction("download")
                .setLabel(translation).setValue(isSuccessful ? elapsedTime : -1)
                .build());
    }

    public static void trackTranslationRemoval(String translation, boolean isSuccessful) {
        sTracker.send(new HitBuilders.EventBuilder()
                .setCategory("translation").setAction("removal")
                .setLabel(translation).setValue(isSuccessful ? 1 : 0)
                .build());
    }

    public static void trackTranslationSelection(String translation) {
        sTracker.send(new HitBuilders.EventBuilder()
                .setCategory("translation").setAction("selection").setLabel(translation)
                .build());
    }

    public static void trackUICopy() {
        trackUIEvent("copy");
    }

    public static void trackUIShare() {
        trackUIEvent("share");
    }

    private static void trackUIEvent(String label) {
        sTracker.send(new HitBuilders.EventBuilder()
                .setCategory("ui").setAction("button").setLabel(label)
                .build());
    }
}