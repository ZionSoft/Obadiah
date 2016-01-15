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

package net.zionsoft.obadiah.billing;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class InAppPurchaseData {
    @IntDef({STATUS_PURCHASED, STATUS_CANCELED, STATUS_REFUNDED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PurchaseStatus {
    }

    public static final int STATUS_PURCHASED = 0;
    public static final int STATUS_CANCELED = 1;
    public static final int STATUS_REFUNDED = 2;

    public final boolean autoRenewing;
    public final String orderId;
    public final String packageName;
    public final String productId;
    public final long purchaseTime;

    @PurchaseStatus
    public final int purchaseState;

    public final String developerPayload;
    public final String purchaseToken;

    public InAppPurchaseData(boolean autoRenewing, String orderId, String packageName,
                             String productId, long purchaseTime, @PurchaseStatus int purchaseState,
                             String developerPayload, String purchaseToken) {
        this.autoRenewing = autoRenewing;
        this.orderId = orderId;
        this.packageName = packageName;
        this.productId = productId;
        this.purchaseTime = purchaseTime;
        this.purchaseState = purchaseState;
        this.developerPayload = developerPayload;
        this.purchaseToken = purchaseToken;
    }
}
