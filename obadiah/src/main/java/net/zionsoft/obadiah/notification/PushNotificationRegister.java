/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2017 ZionSoft
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

package net.zionsoft.obadiah.notification;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.FirebaseMessaging;

public class PushNotificationRegister extends IntentService {
    public static void register(Context context) {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
            return;
        }

        context.startService(new Intent(context, PushNotificationRegister.class));
    }

    public PushNotificationRegister() {
        super("net.zionsoft.obadiah.notification.PushNotificationRegister");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            final FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
            firebaseMessaging.subscribeToTopic("verses");
            firebaseMessaging.subscribeToTopic("newTranslation");
        } catch (Exception ignored) {
        }
    }
}
