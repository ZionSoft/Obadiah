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

package net.zionsoft.obadiah.model.translations;

import android.text.TextUtils;

import net.zionsoft.obadiah.model.TranslationInfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TranslationHelper {
    public static List<TranslationInfo> toTranslationList(JSONArray jsonArray) throws Exception {
        final int length = jsonArray.length();
        final List<TranslationInfo> translations = new ArrayList<TranslationInfo>(length);
        for (int i = 0; i < length; ++i) {
            final JSONObject translationObject = jsonArray.getJSONObject(i);
            final String name = translationObject.getString("name");
            final String shortName = translationObject.getString("shortName");
            final String language = translationObject.getString("language");
            final int size = translationObject.getInt("size");
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(shortName)
                    || TextUtils.isEmpty(language) || size <= 0) {
                throw new Exception("Illegal translation info.");
            }
            translations.add(new TranslationInfo(name, shortName, language, size));
        }
        return translations;
    }
}
