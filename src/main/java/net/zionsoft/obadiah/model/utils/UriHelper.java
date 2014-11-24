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

package net.zionsoft.obadiah.model.utils;

import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.analytics.Analytics;

public class UriHelper {
    public static void checkDeepLink(@NonNull SharedPreferences preferences, @Nullable Uri uri) {
        if (uri == null) {
            return;
        }

        final String path = uri.getPath();
        if (TextUtils.isEmpty(path)) {
            return;
        }

        // format: /bible/<translation-short-name>/<book-index>/<chapter-index>
        final String[] parts = path.split("/");
        if (parts.length < 5) {
            return;
        }

        try {
            final SharedPreferences.Editor editor = preferences.edit();

            // validity of translation short name will be checked later
            final String translationShortName = parts[2];
            if (!TextUtils.isEmpty(translationShortName)) {
                editor.putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, translationShortName);
            } else {
                return;
            }

            final int bookIndex = Integer.parseInt(parts[3]);
            if (bookIndex >= 0 && bookIndex < Bible.getBookCount()) {
                editor.putInt(Constants.PREF_KEY_LAST_READ_BOOK, bookIndex);
            } else {
                return;
            }

            final int chapterIndex = Integer.parseInt(parts[4]);
            if (chapterIndex >= 0 && chapterIndex < Bible.getChapterCount(bookIndex)) {
                editor.putInt(Constants.PREF_KEY_LAST_READ_CHAPTER, chapterIndex);
            } else {
                return;
            }

            editor.putInt(Constants.PREF_KEY_LAST_READ_VERSE, 0).apply();
            Analytics.trackDeepLink();
        } catch (Exception e) {
            Analytics.trackException("Invalid URI: " + uri.toString());
        }
    }
}
