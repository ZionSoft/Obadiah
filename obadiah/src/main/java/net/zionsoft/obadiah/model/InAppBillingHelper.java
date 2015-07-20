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

package net.zionsoft.obadiah.model;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;
import com.crashlytics.android.Crashlytics;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.utils.SimpleAsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

public class InAppBillingHelper implements ServiceConnection {
    public interface OnInitializationFinishedListener {
        public void onInitializationFinished(boolean isSuccessful);
    }

    public interface OnAdsRemovalStateLoadedListener {
        public void onAdsRemovalStateLoaded(boolean isRemoved);
    }

    public interface OnAdsRemovalPurchasedListener {
        public void onAdsRemovalPurchased(boolean isSuccessful);
    }

    private enum Status {
        UNINITIALIZED, INITIALIZING, INITIALIZED, RELEASED
    }

    private static final int BILLING_VERSION = 3;

    private static final int BILLING_RESPONSE_RESULT_OK = 0;
    private static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;

    private static final int REQUEST_PURCHASE = 8964;

    private static final String ITEM_TYPE_INAPP = "inapp";

    private Activity activity;
    private Status status = Status.UNINITIALIZED;
    private IInAppBillingService inAppBillingService;
    private OnInitializationFinishedListener onInitializationFinished;
    private OnAdsRemovalPurchasedListener onAdsRemovalPurchased;

    public void initialize(Activity activity, OnInitializationFinishedListener onInitializationFinished) {
        if (status != Status.UNINITIALIZED)
            return;
        status = Status.INITIALIZING;

        this.activity = activity;
        this.onInitializationFinished = onInitializationFinished;
        activity.bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND")
                        .setPackage("com.android.vending"),
                this, Context.BIND_AUTO_CREATE);
    }

    public void cleanup() {
        if (status != Status.INITIALIZED)
            return;
        status = Status.RELEASED;

        activity.unbindService(this);
    }

    public void loadAdsRemovalState(final OnAdsRemovalStateLoadedListener onLoaded) {
        if (status != Status.INITIALIZED)
            return;

        new SimpleAsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    final Bundle purchases = inAppBillingService.getPurchases(BILLING_VERSION,
                            activity.getPackageName(), ITEM_TYPE_INAPP, null);
                    final int response = purchases.getInt("RESPONSE_CODE");
                    if (response != BILLING_RESPONSE_RESULT_OK) {
                        Analytics.trackException("Failed to load purchases - " + response);
                        return false;
                    }
                    final String adsProductId = activity.getString(R.string.in_app_product_no_ads);
                    for (String data : purchases.getStringArrayList("INAPP_PURCHASE_DATA_LIST")) {
                        final JSONObject purchaseObject = new JSONObject(data);
                        if (purchaseObject.getString("productId").equals(adsProductId)
                                && purchaseObject.getInt("purchaseState") == 0) {
                            return true;
                        }
                    }
                    return false;
                } catch (Exception e) {
                    Crashlytics.getInstance().core.logException(e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (status != InAppBillingHelper.Status.INITIALIZED)
                    return;

                onLoaded.onAdsRemovalStateLoaded(result);
            }
        }.start();
    }

    public void purchaseAdsRemoval(final OnAdsRemovalPurchasedListener onPurchased) {
        if (status != Status.INITIALIZED) {
            Analytics.trackException("Failed to purchase ads removal - Not initialized");
            onPurchased.onAdsRemovalPurchased(false);
            return;
        }

        try {
            // TODO verifies signature

            final Bundle buyIntent = inAppBillingService.getBuyIntent(BILLING_VERSION,
                    activity.getPackageName(), activity.getString(R.string.in_app_product_no_ads),
                    ITEM_TYPE_INAPP, null);
            final int response = buyIntent.getInt("RESPONSE_CODE");
            if (response == BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                onPurchased.onAdsRemovalPurchased(true);
                return;
            }
            if (response != BILLING_RESPONSE_RESULT_OK) {
                Analytics.trackException("Failed to purchase ads removal - " + response);
                onPurchased.onAdsRemovalPurchased(false);
                return;
            }

            onAdsRemovalPurchased = onPurchased;
            final PendingIntent pendingIntent = buyIntent.getParcelable("BUY_INTENT");
            activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
                    REQUEST_PURCHASE, new Intent(), 0, 0, 0);
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
            onPurchased.onAdsRemovalPurchased(false);
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_PURCHASE)
            return false;

        if (status != InAppBillingHelper.Status.INITIALIZED)
            return true;

        if (resultCode != Activity.RESULT_OK) {
            onAdsRemovalPurchased.onAdsRemovalPurchased(false);
            onAdsRemovalPurchased = null;
            return true;
        }

        final int response = data.getIntExtra("RESPONSE_CODE", 0);
        if (response != BILLING_RESPONSE_RESULT_OK
                && response != BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
            Analytics.trackException("Failed to purchase ads removal - " + response);
            onAdsRemovalPurchased.onAdsRemovalPurchased(false);
            onAdsRemovalPurchased = null;
            return true;
        }

        try {
            final JSONObject purchaseObject = new JSONObject(data.getStringExtra("INAPP_PURCHASE_DATA"));
            final String productId = purchaseObject.getString("productId");
            final boolean isPurchased = productId.equals(activity.getString(R.string.in_app_product_no_ads))
                    && purchaseObject.getInt("purchaseState") == 0;
            if (isPurchased)
                Analytics.trackBillingPurchase("remove_ads");
            onAdsRemovalPurchased.onAdsRemovalPurchased(isPurchased);
            onAdsRemovalPurchased = null;
        } catch (JSONException e) {
            Crashlytics.getInstance().core.logException(e);
            onAdsRemovalPurchased.onAdsRemovalPurchased(false);
            onAdsRemovalPurchased = null;
        }

        return true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (status == Status.RELEASED)
            return;
        status = Status.INITIALIZED;

        boolean result = false;
        inAppBillingService = IInAppBillingService.Stub.asInterface(service);
        try {
            final int response = inAppBillingService.isBillingSupported(
                    BILLING_VERSION, activity.getPackageName(), ITEM_TYPE_INAPP);
            if (response == BILLING_RESPONSE_RESULT_OK)
                result = true;
            else
                Analytics.trackBillingNotSupported(response);
        } catch (RemoteException e) {
            Crashlytics.getInstance().core.logException(e);
        }
        if (onInitializationFinished != null) {
            onInitializationFinished.onInitializationFinished(result);
            onInitializationFinished = null;
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        inAppBillingService = null;
    }
}
