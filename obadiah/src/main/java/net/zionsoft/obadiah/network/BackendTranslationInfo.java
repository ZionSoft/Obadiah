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

package net.zionsoft.obadiah.network;

import com.squareup.moshi.Json;

import java.util.List;

/**
 * Used to parse the books.json file.
 */
public class BackendTranslationInfo {
    @Json(name = "name")
    public final String name;

    @Json(name = "shortName")
    public final String shortName;

    @Json(name = "language")
    public final String language;

    @Json(name = "books")
    public final List<String> books;

    public BackendTranslationInfo(String name, String shortName, String language, List<String> books) {
        this.name = name;
        this.shortName = shortName;
        this.language = language;
        this.books = books;
    }
}
