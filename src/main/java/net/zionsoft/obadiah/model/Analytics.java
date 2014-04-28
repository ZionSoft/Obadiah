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

    public static void trackBillingNotSupported(int reason) {
        sTracker.send(new HitBuilders.EventBuilder("billing", "not_supported")
                .setLabel(Integer.toString(reason))
                .build());
    }

    public static void trackScreen(String name) {
        sTracker.setScreenName(name);
        sTracker.send(new HitBuilders.AppViewBuilder().build());
    }

    public static void trackTranslationListDownloading(boolean isSuccessful, long elapsedTime) {
        sTracker.send(new HitBuilders.EventBuilder("download_translation_list", "")
                .setLabel(Boolean.toString(isSuccessful))
                .build());
        if (isSuccessful)
            sTracker.send(new HitBuilders.TimingBuilder("download_translation_list", "", elapsedTime).build());
    }

    public static void trackTranslationDownload(String translation, boolean isSuccessful, long elapsedTime) {
        sTracker.send(new HitBuilders.EventBuilder("download_translation", translation)
                .setLabel(Boolean.toString(isSuccessful))
                .build());
        if (isSuccessful)
            sTracker.send(new HitBuilders.TimingBuilder("download_translation", translation, elapsedTime).build());
    }

    public static void trackTranslationRemoval(String translation, boolean isSuccessful) {
        sTracker.send(new HitBuilders.EventBuilder("remove_translation", translation)
                .setLabel(Boolean.toString(isSuccessful))
                .build());
    }

    public static void trackTranslationSelection(String translation) {
        sTracker.send(new HitBuilders.EventBuilder("select_translation", "")
                .setLabel(translation)
                .build());
    }

    public static void trackUICopy() {
        trackUIEvent("copy");
    }

    public static void trackUIShare() {
        trackUIEvent("share");
    }

    public static void trackUIRemoveAds() {
        trackUIEvent("remove_ads");
    }

    private static void trackUIEvent(String label) {
        sTracker.send(new HitBuilders.EventBuilder("ui", "button_click")
                .setLabel(label)
                .build());
    }
}
