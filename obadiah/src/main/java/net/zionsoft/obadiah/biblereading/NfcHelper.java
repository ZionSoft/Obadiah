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

package net.zionsoft.obadiah.biblereading;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

class NfcHelper {
    static void registerNdefMessageCallback(Activity activity,
                                            NfcAdapter.CreateNdefMessageCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || ContextCompat.checkSelfPermission(activity, Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED) {
            // no NFC permission
            return;
        }
        final NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter != null) {
            nfcAdapter.setNdefPushMessageCallback(callback, activity);
        }
    }

    @Nullable
    static NdefMessage createNdefMessage(Context context, String translation,
                                         int book, int chapter, int verse) {
        return new NdefMessage(new NdefRecord[]{
                NdefRecord.createUri(UriHelper.createUri(translation, book, chapter, verse)),
                NdefRecord.createApplicationRecord(context.getPackageName())});
    }
}
