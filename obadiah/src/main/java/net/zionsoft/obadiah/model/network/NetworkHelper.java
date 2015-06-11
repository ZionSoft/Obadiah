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

package net.zionsoft.obadiah.model.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class NetworkHelper {
    public static final String CLIENT_VERSION_URL = "https://z-bible.appspot.com/v1/clientVersion";

    public static final String PRIMARY_TRANSLATIONS_LIST_URL = "https://z-bible.appspot.com/v1/translations";
    public static final String PRIMARY_TRANSLATION_URL_TEMPLATE = "https://z-bible.appspot.com/v1/translation?blobKey=%s";

    public static final String SECONDARY_TRANSLATIONS_LIST_URL = "http://bible.zionsoft.net/translations/list.json";
    public static final String SECONDARY_TRANSLATION_URL_TEMPLATE = "http://bible.zionsoft.net/translations/%s.zip";

    public static boolean isOnline(Context context) {
        final ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static byte[] get(String url) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = getStream(url);
            return readStream(bis);
        } finally {
            if (bis != null)
                bis.close();
        }
    }

    public static BufferedInputStream getStream(String url) throws IOException {
        final HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();
        return new BufferedInputStream(httpConnection.getInputStream());
    }

    private static byte[] readStream(BufferedInputStream bis) throws IOException {
        int read;
        byte[] result = new byte[0];
        final byte[] buffer = new byte[2048];
        while ((read = bis.read(buffer)) > -1) {
            byte[] tmp = new byte[result.length + read];
            System.arraycopy(result, 0, tmp, 0, result.length);
            System.arraycopy(buffer, 0, tmp, result.length, read);
            result = tmp;
        }
        return result;
    }
}
