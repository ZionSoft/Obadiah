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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.android.vending.billing.IInAppBillingService;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.analytics.Analytics;

import org.json.JSONException;
import org.json.JSONObject;

public class InAppBillingHelper implements ServiceConnection {
    public static interface OnInitializationFinishedListener {
        public void onInitializationFinished(boolean isSuccessful);
    }

    public static interface OnAdsRemovalStateLoadedListener {
        public void onAdsRemovalStateLoaded(boolean isRemoved);
    }

    public static interface OnAdsRemovalPurchasedListener {
        public void onAdsRemovalPurchased(boolean isSuccessful);
    }

    private static enum Status {
        UNINITIALIZED, INITIALIZING, INITIALIZED, RELEASED
    }

    private static final int BILLING_VERSION = 3;

    private static final int BILLING_RESPONSE_RESULT_OK = 0;

    private static final int REQUEST_PURCHASE = 8964;

    private static final String ITEM_TYPE_INAPP = "inapp";

    private Activity mContext;
    private Status mStatus = Status.UNINITIALIZED;
    private IInAppBillingService mInAppBillingService;
    private OnInitializationFinishedListener mOnInitializationFinished;
    private OnAdsRemovalPurchasedListener mOnAdsRemovalPurchased;

    public void initialize(Activity context, OnInitializationFinishedListener onInitializationFinished) {
        if (mStatus != Status.UNINITIALIZED)
            return;
        mStatus = Status.INITIALIZING;

        mContext = context;
        mOnInitializationFinished = onInitializationFinished;
        mContext.bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"), this, Context.BIND_AUTO_CREATE);
    }

    public void cleanup() {
        if (mStatus != Status.INITIALIZED)
            return;
        mStatus = Status.RELEASED;

        mContext.unbindService(this);
    }

    public void loadAdsRemovalState(final OnAdsRemovalStateLoadedListener onLoaded) {
        if (mStatus != Status.INITIALIZED)
            return;

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    final Bundle purchases = mInAppBillingService.getPurchases(BILLING_VERSION,
                            mContext.getPackageName(), ITEM_TYPE_INAPP, null);
                    final int response = purchases.getInt("RESPONSE_CODE");
                    if (response != BILLING_RESPONSE_RESULT_OK) {
                        Analytics.trackException("Failed to load purchases - " + response);
                        return false;
                    }
                    final String adsProductId = mContext.getString(R.string.in_app_product_no_ads);
                    for (String data : purchases.getStringArrayList("INAPP_PURCHASE_DATA_LIST")) {
                        final JSONObject purchaseObject = new JSONObject(data);
                        if (purchaseObject.getString("productId").equals(adsProductId)
                                && purchaseObject.getInt("purchaseState") == 0) {
                            return true;
                        }
                    }
                    return false;
                } catch (Exception e) {
                    Analytics.trackException("Failed to load purchases - " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (mStatus != InAppBillingHelper.Status.INITIALIZED)
                    return;

                onLoaded.onAdsRemovalStateLoaded(result);
            }
        }.execute();
    }

    public void purchaseAdsRemoval(final OnAdsRemovalPurchasedListener onPurchased) {
        if (mStatus != Status.INITIALIZED)
            return;

        try {
            // TODO verifies signature

            final Bundle buyIntent = mInAppBillingService.getBuyIntent(BILLING_VERSION,
                    mContext.getPackageName(), mContext.getString(R.string.in_app_product_no_ads),
                    ITEM_TYPE_INAPP, null);
            final int response = buyIntent.getInt("RESPONSE_CODE");
            if (response != BILLING_RESPONSE_RESULT_OK) {
                Analytics.trackException("Failed to purchase ads removal - " + response);
                onPurchased.onAdsRemovalPurchased(false);
                return;
            }

            mOnAdsRemovalPurchased = onPurchased;
            final PendingIntent pendingIntent = buyIntent.getParcelable("BUY_INTENT");
            mContext.startIntentSenderForResult(pendingIntent.getIntentSender(),
                    REQUEST_PURCHASE, new Intent(), 0, 0, 0);
        } catch (Exception e) {
            Analytics.trackException("Failed to purchase ads removal - " + e.getMessage());
            onPurchased.onAdsRemovalPurchased(false);
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_PURCHASE)
            return false;

        if (mStatus != InAppBillingHelper.Status.INITIALIZED)
            return true;

        if (resultCode != Activity.RESULT_OK) {
            mOnAdsRemovalPurchased.onAdsRemovalPurchased(false);
            mOnAdsRemovalPurchased = null;
            return true;
        }

        final int response = data.getIntExtra("RESPONSE_CODE", 0);
        if (response != BILLING_RESPONSE_RESULT_OK) {
            Analytics.trackException("Failed to purchase ads removal - " + response);
            mOnAdsRemovalPurchased.onAdsRemovalPurchased(false);
            mOnAdsRemovalPurchased = null;
            return true;
        }

        try {
            final JSONObject purchaseObject = new JSONObject(data.getStringExtra("INAPP_PURCHASE_DATA"));
            final String productId = purchaseObject.getString("productId");
            final boolean isPurchased = productId.equals(mContext.getString(R.string.in_app_product_no_ads))
                    && purchaseObject.getInt("purchaseState") == 0;
            if (isPurchased)
                Analytics.trackBillingPurchase(productId, "remove_ads", purchaseObject.getString("orderId"));
            mOnAdsRemovalPurchased.onAdsRemovalPurchased(isPurchased);
            mOnAdsRemovalPurchased = null;
        } catch (JSONException e) {
            Analytics.trackException("Failed to purchase ads removal - " + e.getMessage());
            mOnAdsRemovalPurchased.onAdsRemovalPurchased(false);
            mOnAdsRemovalPurchased = null;
        }

        return true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (mStatus == Status.RELEASED)
            return;
        mStatus = Status.INITIALIZED;

        boolean result = false;
        mInAppBillingService = IInAppBillingService.Stub.asInterface(service);
        try {
            final int response = mInAppBillingService.isBillingSupported(
                    BILLING_VERSION, mContext.getPackageName(), ITEM_TYPE_INAPP);
            if (response == BILLING_RESPONSE_RESULT_OK)
                result = true;
            else
                Analytics.trackBillingNotSupported(response);
        } catch (RemoteException e) {
            Analytics.trackException("Failed to check billing support - " + e.getMessage());
        }
        mOnInitializationFinished.onInitializationFinished(result);
        mOnInitializationFinished = null;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mInAppBillingService = null;
    }
}
