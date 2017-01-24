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

package net.zionsoft.obadiah.translations;

import net.zionsoft.obadiah.model.domain.TranslationInfo;
import net.zionsoft.obadiah.mvp.MVPView;

interface TranslationManagementView extends MVPView {
    void onTranslationLoaded(Translations translations);

    void onTranslationLoadFailed();

    void onTranslationRemoved(TranslationInfo translation);

    void onTranslationRemovalFailed(TranslationInfo translation);

    void onTranslationDownloadProgressed(TranslationInfo translation, int progress);

    void onTranslationDownloaded(TranslationInfo translation);

    void onTranslationDownloadFailed(TranslationInfo translation);

    void showAds();

    void hideAds();
}
