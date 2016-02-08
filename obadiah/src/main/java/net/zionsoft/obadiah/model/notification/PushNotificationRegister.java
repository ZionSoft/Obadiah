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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.analytics.Analytics;

public class PushNotificationRegister extends IntentService {
    public static void register(Context context) {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
            return;
        }

        context.startService(new Intent(context, PushNotificationRegister.class));
    }

    public PushNotificationRegister() {
        super("net.zionsoft.obadiah.model.notification.PushNotificationRegister");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            final String token = InstanceID.getInstance(this).getToken(
                    getString(R.string.google_cloud_messaging_sender_id),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            final GcmPubSub gcmPubSub = GcmPubSub.getInstance(this);
            gcmPubSub.subscribe(token, "/topics/verses", null);
            gcmPubSub.subscribe(token, "/topics/newTranslation", null);

            Analytics.trackEvent(Analytics.CATEGORY_NOTIFICATION, Analytics.NOTIFICATION_ACTION_DEVICE_REGISTERED);
        } catch (Exception e) {
            if (!InstanceID.ERROR_SERVICE_NOT_AVAILABLE.equals(e.getMessage())) {
                Crashlytics.getInstance().core.logException(e);
            }
        }
    }
}
