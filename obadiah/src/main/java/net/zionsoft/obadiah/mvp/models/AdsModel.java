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

package net.zionsoft.obadiah.mvp.models;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.squareup.moshi.Moshi;

import net.zionsoft.obadiah.billing.InAppBilling;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class AdsModel implements InAppBilling.OnAdsRemovalPurchasedListener {
    public interface OnAdsRemovalPurchasedListener {
        void onAdsRemovalPurchased(boolean purchased);
    }

    private final InAppBilling inAppBilling;
    private OnAdsRemovalPurchasedListener onAdsRemovalPurchasedListener;

    public AdsModel(Context context, Moshi moshi) {
        inAppBilling = new InAppBilling(context, moshi);
    }

    public Observable<Boolean> shouldHideAds() {
        return Observable.interval(500L, TimeUnit.MILLISECONDS, Schedulers.io())
                .filter(new Func1<Long, Boolean>() {
                    @Override
                    public Boolean call(Long aLong) {
                        return inAppBilling.isReady();
                    }
                }).take(1)
                .timeout(5000L, TimeUnit.MILLISECONDS)
                .map(new Func1<Long, Boolean>() {
                    @Override
                    public Boolean call(Long aLong) {
                        return inAppBilling.hasPurchasedAdsRemoval();
                    }
                });
    }

    public void purchaseAdsRemoval(Activity activity, OnAdsRemovalPurchasedListener listener) {
        onAdsRemovalPurchasedListener = listener;
        inAppBilling.purchaseAdsRemoval(activity, this);
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return inAppBilling.handleActivityResult(requestCode, resultCode, data);
    }

    public void cleanup() {
        inAppBilling.cleanup();
        onAdsRemovalPurchasedListener = null;
    }

    @Override
    public void onAdsRemovalPurchased(boolean purchased) {
        if (onAdsRemovalPurchasedListener != null) {
            onAdsRemovalPurchasedListener.onAdsRemovalPurchased(purchased);
            onAdsRemovalPurchasedListener = null;
        }
    }
}
