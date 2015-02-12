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

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.network.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class PushNotificationRegister extends IntentService {
    public static void register(Context context) {
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
            return;
        }

        context.startService(new Intent(context, PushNotificationRegister.class));
    }

    public PushNotificationRegister() {
        super("net.zionsoft.obadiah.model.notification.PushNotificationRegister");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GoogleCloudMessaging gcm = null;
        try {
            gcm = GoogleCloudMessaging.getInstance(this);
            final String registrationId = gcm.register(getString(R.string.google_cloud_messaging_sender_id));

            final JSONObject jsonObject = new JSONObject();
            jsonObject.put("pushNotificationId", registrationId);
            jsonObject.put("utcOffset", TimeZone.getDefault().getOffset(new Date().getTime()) / 1000);
            jsonObject.put("locale", Locale.getDefault().toString().toLowerCase());
            NetworkHelper.post(NetworkHelper.DEVICE_ACCOUNT_URL, jsonObject.toString());

            Analytics.trackNotificationEvent("device_registered", null);
        } catch (IOException | JSONException e) {
            Crashlytics.logException(e);
        } finally {
            if (gcm != null) {
                gcm.close();
            }
        }
    }
}
