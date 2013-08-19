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

package net.zionsoft.obadiah.util;

import net.zionsoft.obadiah.bible.TranslationInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class NetworkHelper {
    public static List<TranslationInfo> fetchTranslationList() throws IOException, JSONException {
        final byte[] response = get(String.format("%s/translations", BASE_URL));
        final JSONArray replyArray = new JSONArray(new String(response, "UTF8"));
        final int length = replyArray.length();
        final List<TranslationInfo> translations = new ArrayList<TranslationInfo>(length);
        for (int i = 0; i < length; ++i) {
            final JSONObject translationObject = replyArray.getJSONObject(i);
            translations.add(new TranslationInfo(translationObject.getLong("uniqueId"),
                    translationObject.getString("name"), translationObject.getString("shortName"),
                    translationObject.getString("language"), translationObject.getString("blobKey"),
                    translationObject.getInt("size"), translationObject.getLong("timestamp"),
                    false));
        }
        return translations;
    }

    private static byte[] get(String url) throws IOException {
        byte[] result = null;
        HttpsURLConnection httpsConnection = null;
        try {
            httpsConnection = (HttpsURLConnection) new URL(url).openConnection();
            InputStream is = new BufferedInputStream(httpsConnection.getInputStream());
            result = new byte[0];
            byte[] buffer = new byte[2048];
            int read;
            while ((read = is.read(buffer)) > -1) {
                byte[] tmp = new byte[result.length + read];
                System.arraycopy(result, 0, tmp, 0, result.length);
                System.arraycopy(buffer, 0, tmp, result.length, read);
                result = tmp;
            }
        } finally {
            if (httpsConnection != null)
                httpsConnection.disconnect();
        }
        return result;
    }

    private static final String BASE_URL = "https://zionsoft-bible.appspot.com/1.0";
}
