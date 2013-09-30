/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2013 ZionSoft
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

package net.zionsoft.obadiah.bible;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.util.NetworkHelper;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TranslationListDownloadService extends IntentService {
    public static final String ACTION_STATUS_UPDATE
            = "net.zionsoft.obadiah.bible.TranslationListDownloadService.ACTION_STATUS_UPDATE";

    public static final String KEY_STATUS
            = "net.zionsoft.obadiah.bible.TranslationListDownloadService.KEY_STATUS";
    public static final String KEY_TRANSLATION_LIST
            = "net.zionsoft.obadiah.bible.TranslationListDownloadService.KEY_TRANSLATION_LIST";

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_NETWORK_FAILURE = 1;
    public static final int STATUS_SERVER_FAILURE = 2;

    public TranslationListDownloadService() {
        super("net.zionsoft.obadiah.bible.TranslationListDownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int status = STATUS_SUCCESS;
        List<TranslationInfo> translations = null;
        try {
            final TranslationManager translationManager = new TranslationManager(this);
            translationManager.addTranslations(NetworkHelper.fetchTranslationList());
            translations = translationManager.availableTranslations();
        } catch (JSONException e) {
            // malformed server response
            status = STATUS_SERVER_FAILURE;
        } catch (IOException e) {
            // network failure
            status = STATUS_NETWORK_FAILURE;
        } finally {
            getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putBoolean(Constants.PREF_KEY_DOWNLOADING_TRANSLATION_LIST, false).commit();

            final Intent i = new Intent(ACTION_STATUS_UPDATE);
            i.putExtra(KEY_STATUS, status);
            if (status == STATUS_SUCCESS) {
                i.putParcelableArrayListExtra(KEY_TRANSLATION_LIST,
                        (ArrayList<TranslationInfo>) translations);
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        }
    }
}
