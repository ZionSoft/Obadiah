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

import android.os.Parcel;
import android.os.Parcelable;

public class TranslationInfo implements Parcelable {
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

    // Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dst, int flags) {
        dst.writeLong(uniqueId);
        dst.writeString(name);
        dst.writeString(shortName);
        dst.writeString(language);
        dst.writeString(blobKey);
        dst.writeInt(size);
        dst.writeLong(timestamp);
        dst.writeByte((byte) (installed ? 1 : 0));
    }

    public static final Parcelable.Creator<TranslationInfo> CREATOR
            = new Parcelable.Creator<TranslationInfo>() {
        @Override
        public TranslationInfo createFromParcel(Parcel in) {
            return new TranslationInfo(in.readLong(), in.readString(), in.readString(),
                    in.readString(), in.readString(), in.readInt(), in.readLong(),
                    in.readByte() == 1);
        }

        @Override
        public TranslationInfo[] newArray(int size) {
            return new TranslationInfo[size];
        }
    };
}
