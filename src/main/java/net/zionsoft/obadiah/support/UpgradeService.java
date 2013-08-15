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

package net.zionsoft.obadiah.support;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;

import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.bible.TranslationManager;
import net.zionsoft.obadiah.bible.TranslationsDatabaseHelper;
import net.zionsoft.obadiah.util.NetworkHelper;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UpgradeService extends IntentService {
    public static final String ACTION_STATUS_UPDATE
            = "net.zionsoft.obadiah.support.IntentService.ACTION_STATUS_UPDATE";

    public static final String KEY_RESULT_UPGRADE_SUCCESS
            = "net.zionsoft.obadiah.support.IntentService.KEY_RESULT_UPGRADE_SUCCESS";

    public UpgradeService() {
        super("net.zionsoft.obadiah.support.UpgradeService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean success = true;
        final SharedPreferences preferences
                = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        try {
            final int version = preferences.getInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, 0);
            if (version < 10500) {
                // prior to 1.5.0

                // upgrading from prior to 1.5.0 is no longer supported since 1.7.0
                // now simply delete all the old data
                Utils.removeDirectory(getFilesDir());
                editor.remove("selectedTranslation");
            }
            if (version < 10700) {
                // prior to 1.7.0

                // no longer tracks timestamp when translation list is fetched since 1.7.0
                editor.remove("lastUpdated");

                // new database format for translation list is introduced in 1.7.0
                new TranslationManager(this).addTranslations(allTranslations());
            }

            editor.putInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION,
                    Constants.CURRENT_APPLICATION_VERSION);
        } catch (Exception e) {
            e.printStackTrace();
            success = false;
        } finally {
            editor.putBoolean(Constants.PREF_KEY_UPGRADING, false).commit();
        }

        final Intent result = new Intent(ACTION_STATUS_UPDATE);
        result.putExtra(KEY_RESULT_UPGRADE_SUCCESS, success);
        LocalBroadcastManager.getInstance(this).sendBroadcast(result);
    }

    private List<TranslationInfo> allTranslations() throws IOException, JSONException {
        SQLiteDatabase db = new TranslationsDatabaseHelper(this).getReadableDatabase();
        final Cursor cursor = db.query("sqlite_master", new String[]{"name"},
                "type = ?", new String[]{"table"}, null, null, null);
        final int columnIndex = cursor.getColumnIndex("name");
        List<String> tableNames = new ArrayList<String>(cursor.getCount());
        while (cursor.moveToNext())
            tableNames.add(cursor.getString(columnIndex));
        db.close();

        List<TranslationInfo> translations = NetworkHelper.fetchTranslationList();
        for (TranslationInfo translationInfo : translations) {
            for (String name : tableNames) {
                if (translationInfo.shortName.equals(name)) {
                    translationInfo.installed = true;
                    break;
                }
            }
        }
        return translations;
    }
}
