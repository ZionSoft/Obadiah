/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2017 ZionSoft
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

import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseUserActions;

import net.zionsoft.obadiah.BuildConfig;

abstract class AppIndexingManager {
    private static final StringBuilder STRING_BUILDER = new StringBuilder();

    private static Action action;

    static void onView(String translationShortName, String bookName, int bookIndex, int chapterIndex) {
        onViewEnd();

        final String title;
        final String appUrl;
        final String webUrl;
        synchronized (STRING_BUILDER) {
            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(bookName).append(", ").append(chapterIndex + 1)
                    .append(" (").append(translationShortName).append(')');
            title = STRING_BUILDER.toString();

            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(BuildConfig.DEBUG
                    ? "android-app://net.zionsoft.obadiah.debug/http/bible.zionsoft.net/bible/"
                    : "android-app://net.zionsoft.obadiah/http/bible.zionsoft.net/bible/")
                    .append(translationShortName)
                    .append('/').append(bookIndex).append('/').append(chapterIndex);
            appUrl = STRING_BUILDER.toString();

            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append("http://bible.zionsoft.net/bible/").append(translationShortName)
                    .append('/').append(bookIndex).append('/').append(chapterIndex);
            webUrl = STRING_BUILDER.toString();
        }

        action = new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(title, appUrl, webUrl)
                .build();
        FirebaseUserActions.getInstance().start(action);
    }

    static void onViewEnd() {
        if (action != null) {
            FirebaseUserActions.getInstance().end(action);
            action = null;
        }
    }
}
