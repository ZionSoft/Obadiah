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

import android.support.annotation.NonNull;

import com.squareup.moshi.Json;

import net.zionsoft.obadiah.model.domain.TranslationInfo;

class BackendTranslationInfo {
    @Json(name = "name")
    final String name;

    @Json(name = "shortName")
    final String shortName;

    @Json(name = "language")
    final String language;

    @Json(name = "blobKey")
    final String blobKey;

    @Json(name = "size")
    final int size;

    BackendTranslationInfo(String name, String shortName, String language, String blobKey, int size) {
        super();

        this.name = name;
        this.shortName = shortName;
        this.language = language;
        this.blobKey = blobKey;
        this.size = size;
    }

    @NonNull
    TranslationInfo toTranslationInfo() {
        return TranslationInfo.create(name, shortName, language, blobKey, size);
    }
}
