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

import android.content.Context;
import android.net.Uri;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;

import net.zionsoft.obadiah.BuildConfig;

class AppIndexingManager {
    private static final StringBuilder STRING_BUILDER = new StringBuilder();

    private final GoogleApiClient googleApiClient;
    private Action action;

    AppIndexingManager(Context context) {
        super();

        if (ConnectionResult.SUCCESS != GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context)) {
            // no need to bother the user to install latest Google Play services
            googleApiClient = null;
            return;
        }

        googleApiClient = new GoogleApiClient.Builder(context).addApi(AppIndex.API).build();
    }

    void onStart() {
        if (googleApiClient != null)
            googleApiClient.connect();
    }

    void onView(String translationShortName, String bookName, int bookIndex, int chapterIndex) {
        if (googleApiClient != null) {
            onViewEnd();

            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(bookName).append(", ").append(chapterIndex + 1)
                    .append(" (").append(translationShortName).append(')');
            final String title = STRING_BUILDER.toString();

            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append("http://bible.zionsoft.net/bible/").append(translationShortName)
                    .append('/').append(bookIndex).append('/').append(chapterIndex);
            final Uri webUri = Uri.parse(STRING_BUILDER.toString());

            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(BuildConfig.DEBUG
                    ? "android-app://net.zionsoft.obadiah.debug/http/bible.zionsoft.net/bible/"
                    : "android-app://net.zionsoft.obadiah/http/bible.zionsoft.net/bible/")
                    .append(translationShortName)
                    .append('/').append(bookIndex).append('/').append(chapterIndex);
            final Uri appUri = Uri.parse(STRING_BUILDER.toString());

            action = Action.newAction(Action.TYPE_VIEW, title, webUri, appUri);
            AppIndex.AppIndexApi.start(googleApiClient, action);
        }
    }

    private void onViewEnd() {
        if (action != null) {
            AppIndex.AppIndexApi.end(googleApiClient, action);
            action = null;
        }
    }

    public void onStop() {
        if (googleApiClient != null) {
            onViewEnd();
            googleApiClient.disconnect();
        }
    }
}
