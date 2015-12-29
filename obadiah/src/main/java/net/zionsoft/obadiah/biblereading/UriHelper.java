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

package net.zionsoft.obadiah.biblereading;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;

import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.domain.Bible;

class UriHelper {
    // format: /bible/<translation-short-name>/<book-index>/<chapter-index>/<verse-index>
    private static final String VERSE_URI_TEMPLATE = "https://bible.zionsoft.net/bible/%s/%d/%d/%d";

    static void checkUri(@NonNull BibleReadingPresenter bibleReadingPresenter, @NonNull Uri uri) {
        // TODO supports verse index

        final String path = uri.getPath();
        if (TextUtils.isEmpty(path)) {
            return;
        }

        final String[] parts = path.split("/");
        if (parts.length < 5) {
            return;
        }

        try {
            final String translationShortName = parts[2];
            final int bookIndex = Integer.parseInt(parts[3]);
            final int chapterIndex = Integer.parseInt(parts[4]);

            // validity of translation short name will be checked later when loading the available
            // translation list
            if (TextUtils.isEmpty(translationShortName)
                    || bookIndex < 0 || bookIndex >= Bible.getBookCount()
                    || chapterIndex < 0 || chapterIndex >= Bible.getChapterCount(bookIndex)) {
                return;
            }

            bibleReadingPresenter.saveCurrentTranslation(translationShortName);
            bibleReadingPresenter.saveReadingProgress(bookIndex, chapterIndex, 0);
            Analytics.trackEvent(Analytics.CATEGORY_DEEP_LINK, Analytics.DEEP_LINK_ACTION_OPENED);
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
        }
    }

    @NonNull
    static String createUri(String translation, int book, int chapter, int verse) {
        return String.format(VERSE_URI_TEMPLATE, translation, book, chapter, verse);
    }
}
