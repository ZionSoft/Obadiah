/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2013 ZionSoft
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

package net.zionsoft.obadiah.bible;

public class TranslationInfo {
    public final long uniqueId;
    public final String name;
    public final String shortName;
    public final String language;
    public final String blobKey;
    public final int size;
    public final long timestamp;
    public boolean installed;

    public TranslationInfo(long uniqueId, String name, String shortName, String language,
                           String blobKey, int size, long timestamp, boolean installed) {
        super();
        this.uniqueId = uniqueId;
        this.name = name;
        this.shortName = shortName;
        this.language = language;
        this.blobKey = blobKey;
        this.size = size;
        this.timestamp = timestamp;
        this.installed = installed;
    }
}
