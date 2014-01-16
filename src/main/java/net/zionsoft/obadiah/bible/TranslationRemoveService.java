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

package net.zionsoft.obadiah.bible;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import net.zionsoft.obadiah.Constants;

public class TranslationRemoveService extends IntentService {
    public static final String ACTION_STATUS_UPDATE
            = "net.zionsoft.obadiah.bible.TranslationRemoveService.ACTION_STATUS_UPDATE";

    public static final String KEY_STATUS
            = "net.zionsoft.obadiah.bible.TranslationRemoveService.KEY_STATUS";
    public static final String KEY_TRANSLATION
            = "net.zionsoft.obadiah.bible.TranslationRemoveService.KEY_TRANSLATION";

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_FAILURE = 1;

    public TranslationRemoveService() {
        super("net.zionsoft.obadiah.bible.TranslationRemoveService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int status = STATUS_SUCCESS;
        try {
            final TranslationInfo translation = intent.getParcelableExtra(KEY_TRANSLATION);
            new TranslationManager(this).removeTranslation(translation);
        } catch (IllegalArgumentException e) {
            status = STATUS_FAILURE;
        } finally {
            getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putBoolean(Constants.PREF_KEY_REMOVING_TRANSLATION, false).commit();

            LocalBroadcastManager.getInstance(this).sendBroadcast(
                    new Intent(ACTION_STATUS_UPDATE).putExtra(KEY_STATUS, status));
        }
    }
}
