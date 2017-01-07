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

package net.zionsoft.obadiah;

import android.net.Uri;

public class Constants {
    public static final String PREF_NAME = "settings";
    public static final String PREF_KEY_CURRENT_APPLICATION_VERSION = "currentApplicationVersion";
    public static final String PREF_KEY_LAST_READ_TRANSLATION = "currentTranslation";
    public static final String PREF_KEY_LAST_READ_BOOK = "currentBook";
    public static final String PREF_KEY_LAST_READ_CHAPTER = "currentChapter";
    public static final String PREF_KEY_LAST_READ_VERSE = "currentVerse";

    public static final Uri GOOGLE_PLAY_URI = Uri.parse("market://details?id=net.zionsoft.obadiah");

    public static final Uri FACEBOOK_PAGE_NEW_URI = Uri.parse("fb://facewebmodal/f?href=https://www.facebook.com/WeReadBible/");
    public static final Uri FACEBOOK_PAGE_OLD_URI = Uri.parse("fb://page/WeReadBible/");
    public static final Uri FACEBOOK_PAGE_FALLBACK_URI = Uri.parse("https://www.facebook.com/WeReadBible/");

    public static final String BASE_URL = "https://z-bible.appspot.com/v1/";
}
