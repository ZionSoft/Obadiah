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

package net.zionsoft.obadiah.model.appindexing;

import android.app.Activity;
import android.net.Uri;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;

import net.zionsoft.obadiah.BuildConfig;

public class AppIndexingManager {
    private final Activity activity;
    private final GoogleApiClient googleApiClient;

    private static final String TITLE_TEMPLATE = "%s, %d (%s)";
    private static final String APP_URI_TEMPLATE = BuildConfig.DEBUG
            ? "android-app://net.zionsoft.obadiah.debug/http/bible.zionsoft.net/bible/%s/%d/%d"
            : "android-app://net.zionsoft.obadiah/http/bible.zionsoft.net/bible/%s/%d/%d";
    private static final String WEB_URI_TEMPLATE = "http://bible.zionsoft.net/bible/%s/%d/%d";
    private Uri mAppIndexingUri;

    public AppIndexingManager(Activity activity) {
        super();

        if (ConnectionResult.SUCCESS != GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity)) {
            // no need to bother the user to install latest Google Play services
            this.activity = null;
            googleApiClient = null;
            return;
        }

        this.activity = activity;
        googleApiClient = new GoogleApiClient.Builder(activity).addApi(AppIndex.APP_INDEX_API).build();
    }

    public void onStart() {
        if (googleApiClient != null)
            googleApiClient.connect();
    }

    public void onView(String translationShortName, String bookName, int bookIndex, int chapterIndex) {
        if (googleApiClient != null) {
            onViewEnd();

            // TODO add out links

            mAppIndexingUri = Uri.parse(String.format(APP_URI_TEMPLATE, translationShortName, bookIndex, chapterIndex));
            AppIndex.AppIndexApi.view(googleApiClient, activity, mAppIndexingUri,
                    String.format(TITLE_TEMPLATE, translationShortName, chapterIndex + 1, bookName),
                    Uri.parse(String.format(WEB_URI_TEMPLATE, translationShortName, bookIndex, chapterIndex)), null);
        }
    }

    private void onViewEnd() {
        if (mAppIndexingUri != null)
            AppIndex.AppIndexApi.viewEnd(googleApiClient, activity, mAppIndexingUri);
    }

    public void onStop() {
        if (googleApiClient != null) {
            onViewEnd();
            googleApiClient.disconnect();
        }
    }
}
