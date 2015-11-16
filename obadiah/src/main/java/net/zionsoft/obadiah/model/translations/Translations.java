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

package net.zionsoft.obadiah.model.translations;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Translations {
    @NonNull
    public final List<TranslationInfo> downloaded;

    @NonNull
    public final List<TranslationInfo> available;

    private Translations(@NonNull List<TranslationInfo> downloaded, @NonNull List<TranslationInfo> available) {
        this.downloaded = downloaded;
        this.available = available;
    }

    public static class Builder {
        private List<TranslationInfo> translations;
        private List<String> downloaded;

        public Builder translations(List<TranslationInfo> translations) {
            this.translations = translations;
            return this;
        }

        public Builder downloaded(List<String> downloaded) {
            this.downloaded = downloaded;
            return this;
        }

        public Translations build() {
            final int translationCount = translations.size();
            final int downloadedCount = downloaded.size();
            final List<TranslationInfo> downloadedTranslations = new ArrayList<>(downloadedCount);
            final List<TranslationInfo> availableTranslations = new ArrayList<>(translationCount - downloadedCount);
            for (int i = 0; i < translationCount; ++i) {
                final TranslationInfo translation = translations.get(i);
                boolean isDownloaded = false;
                for (int j = 0; j < downloadedCount; ++j) {
                    if (translation.shortName.equals(downloaded.get(j))) {
                        isDownloaded = true;
                        break;
                    }
                }
                if (isDownloaded) {
                    downloadedTranslations.add(translation);
                } else {
                    availableTranslations.add(translation);
                }
            }
            return new Translations(Collections.unmodifiableList(downloadedTranslations),
                    Collections.unmodifiableList(availableTranslations));
        }
    }
}
