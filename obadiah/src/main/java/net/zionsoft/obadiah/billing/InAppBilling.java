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

package net.zionsoft.obadiah.billing;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IntDef;

import com.android.vending.billing.IInAppBillingService;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.crash.Crash;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class InAppBilling implements ServiceConnection {
    public interface OnAdsRemovalPurchasedListener {
        void onAdsRemovalPurchased(boolean purchased);
    }

    @IntDef({STATUS_UNKNOWN, STATUS_INITIALIZING, STATUS_READY, STATUS_RELEASED, STATUS_UNSUPPORTED})
    @Retention(RetentionPolicy.SOURCE)
    @interface Status {
    }

    private static final int STATUS_UNKNOWN = 0;
    private static final int STATUS_INITIALIZING = 1;
    private static final int STATUS_READY = 2;
    private static final int STATUS_RELEASED = 3;
    private static final int STATUS_UNSUPPORTED = 4;

    private static final int BILLING_VERSION = 3;

    private static final int BILLING_RESPONSE_RESULT_OK = 0;
    private static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;

    private static final int REQUEST_PURCHASE = 8964;

    private static final String ITEM_TYPE_INAPP = "inapp";

    private final Context applicationContext;
    private final JsonAdapter<InAppPurchaseData> inAppPurchaseDataJsonAdapter;

    private IInAppBillingService inAppBillingService;
    private OnAdsRemovalPurchasedListener onAdsRemovalPurchasedListener;

    @Status
    private int status = STATUS_UNKNOWN;

    public InAppBilling(Context context, Moshi moshi) {
        applicationContext = context.getApplicationContext();
        inAppPurchaseDataJsonAdapter = moshi.adapter(InAppPurchaseData.class);

        status = STATUS_INITIALIZING;

        applicationContext.bindService(
                new Intent("com.android.vending.billing.InAppBillingService.BIND").setPackage("com.android.vending"),
                this, Context.BIND_AUTO_CREATE);
    }

    public boolean isReady() {
        return status == STATUS_READY;
    }

    public void cleanup() {
        if (status != STATUS_READY) {
            return;
        }
        status = STATUS_RELEASED;

        applicationContext.unbindService(this);
        onAdsRemovalPurchasedListener = null;
    }

    public void purchaseAdsRemoval(Activity activity, OnAdsRemovalPurchasedListener listener) {
        if (status != STATUS_READY) {
            listener.onAdsRemovalPurchased(false);
            return;
        }

        try {
            // TODO verifies signature

            final Bundle buyIntent = inAppBillingService.getBuyIntent(BILLING_VERSION,
                    applicationContext.getPackageName(),
                    applicationContext.getString(R.string.in_app_product_no_ads),
                    ITEM_TYPE_INAPP, null);
            final int response = buyIntent.getInt("RESPONSE_CODE");
            if (response == BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                listener.onAdsRemovalPurchased(true);
                return;
            }
            if (response != BILLING_RESPONSE_RESULT_OK) {
                Crash.report(new InAppBillingException("Failed to purchase ads removal: " + response));
                listener.onAdsRemovalPurchased(false);
                return;
            }

            final PendingIntent pendingIntent = buyIntent.getParcelable("BUY_INTENT");
            final IntentSender intentSender = pendingIntent != null ? pendingIntent.getIntentSender() : null;
            if (intentSender == null) {
                listener.onAdsRemovalPurchased(false);
            } else {
                onAdsRemovalPurchasedListener = listener;
                activity.startIntentSenderForResult(intentSender, REQUEST_PURCHASE, new Intent(), 0, 0, 0);
            }
        } catch (Exception e) {
            Crash.report(e);
            listener.onAdsRemovalPurchased(false);
        }
    }

    public boolean hasPurchasedAdsRemoval() {
        if (status != STATUS_READY) {
            return false;
        }

        try {
            final Bundle purchases = inAppBillingService.getPurchases(BILLING_VERSION,
                    applicationContext.getPackageName(), ITEM_TYPE_INAPP, null);
            final int response = purchases.getInt("RESPONSE_CODE");
            if (response != BILLING_RESPONSE_RESULT_OK) {
                Crash.report(new InAppBillingException("Failed to load purchases: " + response));
                return false;
            }
            final String adsProductId = applicationContext.getString(R.string.in_app_product_no_ads);
            final ArrayList<String> purchaseData = purchases.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
            if (purchaseData != null) {
                final int size = purchaseData.size();
                for (int i = 0; i < size; ++i) {
                    final InAppPurchaseData inAppPurchaseData
                            = inAppPurchaseDataJsonAdapter.fromJson(purchaseData.get(i));
                    if (inAppPurchaseData.productId.equals(adsProductId)
                            && inAppPurchaseData.purchaseState == InAppPurchaseData.STATUS_PURCHASED) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            Crash.report(e);
            return false;
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_PURCHASE) {
            return false;
        }

        if (status != STATUS_READY) {
            return true;
        }

        if (resultCode != Activity.RESULT_OK) {
            informAdsRemovalPurchased(false);
            return true;
        }

        final int response = data.getIntExtra("RESPONSE_CODE", 0);
        if (response != BILLING_RESPONSE_RESULT_OK
                && response != BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
            Crash.report(new InAppBillingException("Failed to purchase ads removal: " + response));
            informAdsRemovalPurchased(false);
            return true;
        }

        try {
            final InAppPurchaseData inAppPurchaseData
                    = inAppPurchaseDataJsonAdapter.fromJson(data.getStringExtra("INAPP_PURCHASE_DATA"));
            final boolean isPurchased =
                    inAppPurchaseData.productId.equals(applicationContext.getString(R.string.in_app_product_no_ads))
                            && inAppPurchaseData.purchaseState == InAppPurchaseData.STATUS_PURCHASED;
            informAdsRemovalPurchased(isPurchased);
        } catch (IOException e) {
            Crash.report(e);
            informAdsRemovalPurchased(false);
        }

        return true;
    }

    private void informAdsRemovalPurchased(boolean purchased) {
        if (onAdsRemovalPurchasedListener != null) {
            onAdsRemovalPurchasedListener.onAdsRemovalPurchased(purchased);
            onAdsRemovalPurchasedListener = null;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (status != STATUS_INITIALIZING) {
            return;
        }

        inAppBillingService = IInAppBillingService.Stub.asInterface(service);
        try {
            final int response = inAppBillingService.isBillingSupported(
                    BILLING_VERSION, applicationContext.getPackageName(), ITEM_TYPE_INAPP);
            if (response == BILLING_RESPONSE_RESULT_OK) {
                status = STATUS_READY;
            } else {
                status = STATUS_UNSUPPORTED;
                Crash.report(new InAppBillingException("In-app billing not supported: " + response));
            }
        } catch (Exception e) {
            status = STATUS_UNSUPPORTED;
            Crash.report(e);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        inAppBillingService = null;
        status = STATUS_RELEASED;
    }
}
