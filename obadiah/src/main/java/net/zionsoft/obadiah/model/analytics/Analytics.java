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
    public static final String BILLING_ACTION_ERROR = "error";

    public static final String CATEGORY_DEEP_LINK = "deep_link";
    public static final String DEEP_LINK_ACTION_OPENED = "opened";

    public static final String CATEGORY_NOTIFICATION = "notification";
    public static final String NOTIFICATION_ACTION_SHOWN = "shown";
    public static final String NOTIFICATION_ACTION_OPENED = "opened";
    public static final String NOTIFICATION_ACTION_DISMISSED = "dismissed";

    public static final String CATEGORY_UI = "ui";
    public static final String UI_ACTION_BUTTON_CLICK = "button_click";

    public static final String CATEGORY_TRANSLATION = "translation";
    public static final String TRANSLATION_ACTION_LIST_DOWNLOADED = "list_downloaded";
    public static final String TRANSLATION_ACTION_DOWNLOADED = "downloaded";
    public static final String TRANSLATION_ACTION_SELECTED = "selected";
    public static final String TRANSLATION_ACTION_REMOVED = "removed";
    public static final String TRANSLATION_ACTION_PARALLEL_SELECTED = "parallel_selected";

    public static final String CATEGORY_SOCIAL = "social";
    public static final String SOCIAL_ACTION_INVITE_FRIENDS = "invite_friends";

    public static final String CATEGORY_NFC = "nfc";
    public static final String NFC_ACTION_VERSE = "verse";

    public static final String CATEGORY_BOOKMARKS = "bookmarks";
    public static final String BOOKMARKS_ACTION_ADDED = "added";
    public static final String BOOKMARKS_ACTION_REMOVED = "removed";
    public static final String BOOKMARKS_ACTION_OPENED = "opened";

    public static final String CATEGORY_NOTES = "notes";
    public static final String NOTES_ACTION_ADDED = "added";
    public static final String NOTES_ACTION_REMOVED = "removed";
    public static final String NOTES_ACTION_OPENED = "opened";

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
}
