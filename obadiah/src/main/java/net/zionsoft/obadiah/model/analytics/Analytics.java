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

package net.zionsoft.obadiah.model.analytics;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import net.zionsoft.obadiah.R;

public class Analytics {
    public static final String CATEGORY_BILLING = "billing";
    public static final String BILLING_ACTION_NOT_SUPPORTED = "not_supported";
    public static final String BILLING_ACTION_PURCHASED = "purchased";
    public static final String BILLING_ACTION_ERROR = "error";

    private static Tracker tracker;

    public static void initialize(Context context) {
        if (tracker == null) {
            synchronized (Analytics.class) {
                if (tracker == null) {
                    tracker = GoogleAnalytics.getInstance(context).newTracker(R.xml.analytics);
                }
            }
        }
    }

    public static void trackEvent(@NonNull String category, @NonNull String action) {
        trackEvent(category, action, null, 1L);
    }

    public static void trackEvent(@NonNull String category, @NonNull String action, @NonNull String label) {
        trackEvent(category, action, label, 1L);
    }

    public static void trackEvent(@NonNull String category, @NonNull String action, @Nullable String label, long value) {
        final HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder(category, action);
        if (!TextUtils.isEmpty(label)) {
            eventBuilder.setLabel(label);
        }
        if (value >= 0L) {
            eventBuilder.setValue(value);
        }
        tracker.send(eventBuilder.build());
    }

    public static void trackScreen(String name) {
        tracker.setScreenName(name);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public static void trackTranslationListDownloading(boolean isSuccessful, long elapsedTime) {
        tracker.send(new HitBuilders.EventBuilder("download_translation_list", "")
                .setLabel(Boolean.toString(isSuccessful))
                .build());
        if (isSuccessful)
            tracker.send(new HitBuilders.TimingBuilder("download_translation_list", "", elapsedTime).build());
    }

    public static void trackTranslationDownload(String translation, boolean isSuccessful, long elapsedTime) {
        tracker.send(new HitBuilders.EventBuilder("download_translation", translation)
                .setLabel(Boolean.toString(isSuccessful))
                .build());
        if (isSuccessful)
            tracker.send(new HitBuilders.TimingBuilder("download_translation", translation, elapsedTime).build());
    }

    public static void trackTranslationRemoval(String translation, boolean isSuccessful) {
        tracker.send(new HitBuilders.EventBuilder("remove_translation", translation)
                .setLabel(Boolean.toString(isSuccessful))
                .build());
    }

    public static void trackTranslationSelection(String translation) {
        tracker.send(new HitBuilders.EventBuilder("select_translation", translation)
                .build());
    }

    public static void trackDeepLink() {
        tracker.send(new HitBuilders.EventBuilder("deep_link", "open")
                .build());
    }

    public static void trackUIEvent(String label) {
        tracker.send(new HitBuilders.EventBuilder("ui", "button_click")
                .setLabel(label)
                .build());
    }

    public static void trackNotificationEvent(String action, String label) {
        tracker.send(new HitBuilders.EventBuilder("notification", action)
                .setLabel(label)
                .build());
    }
}
