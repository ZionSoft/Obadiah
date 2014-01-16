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

package net.zionsoft.obadiah.util;

import net.zionsoft.obadiah.bible.TranslationInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class NetworkHelper {
    public static List<TranslationInfo> fetchTranslationList() throws IOException, JSONException {
        BufferedInputStream bis = null;
        try {
            bis = get(String.format("translations?locale=%s",
                    Locale.getDefault().toString().toLowerCase()));
            byte[] response = new byte[0];
            byte[] buffer = new byte[2048];
            int read;
            while ((read = bis.read(buffer)) > -1) {
                byte[] tmp = new byte[response.length + read];
                System.arraycopy(response, 0, tmp, 0, response.length);
                System.arraycopy(buffer, 0, tmp, response.length, read);
                response = tmp;
            }

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
        } finally {
            if (bis != null)
                bis.close();
        }
    }

    public static BufferedInputStream get(String url) throws IOException {
        HttpsURLConnection httpsConnection = (HttpsURLConnection) new URL(String
                .format("https://zionsoft-bible.appspot.com/1.0/%s", url)).openConnection();
        return new BufferedInputStream(httpsConnection.getInputStream());
    }
}
