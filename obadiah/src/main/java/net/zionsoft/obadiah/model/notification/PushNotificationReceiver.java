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

package net.zionsoft.obadiah.model.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import net.zionsoft.obadiah.model.analytics.Analytics;

public class PushNotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // should just use GcmReceiver, but it requires the WAKE_LOCK permission
        final String messageType = intent.getStringExtra("message_type");
        Analytics.trackEvent(Analytics.CATEGORY_NOTIFICATION, Analytics.NOTIFICATION_ACTION_RECEIVED,
                TextUtils.isEmpty(messageType) ? "empty message type" : messageType);
        if (TextUtils.isEmpty(messageType) || "gcm".equals(messageType)) {
            context.startService(PushNotificationHandler.newStartIntent(context, intent.getExtras()));
        }
    }
}
