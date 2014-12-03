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

package net.zionsoft.obadiah.model.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.zionsoft.obadiah.model.analytics.Analytics;

public class PushDismissedReceiver extends BroadcastReceiver {
    private static final String KEY_MESSAGE_TYPE = "net.zionsoft.obadiah.model.notification.PushDismissedReceiver.KEY_MESSAGE_TYPE";

    public static Intent newStartIntent(Context context, String messageType) {
        return new Intent(context, PushDismissedReceiver.class)
                .putExtra(KEY_MESSAGE_TYPE, messageType);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Analytics.trackNotificationEvent("notification_dismissed", intent.getStringExtra(KEY_MESSAGE_TYPE));
    }
}
