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
    public TranslationInfo(long uniqueId, String name, String shortName, String language,
                           String blobKey, int size, long timestamp, boolean installed) {
        super();
        mUniqueId = uniqueId;
        mName = name;
        mShortName = shortName;
        mLanguage = language;
        mBlobKey = blobKey;
        mSize = size;
        mTimestamp = timestamp;
        mInstalled = installed;
    }

    public long uniqueId() {
        return mUniqueId;
    }

    public String name() {
        return mName;
    }

    public String shortName() {
        return mShortName;
    }

    public String language() {
        return mLanguage;
    }

    public String blobKey() {
        return mBlobKey;
    }

    public int size() {
        return mSize;
    }

    public long timestamp() {
        return mTimestamp;
    }

    public boolean installed() {
        return mInstalled;
    }

    // TODO should be package access only
    public void setInstalled(boolean installed) {
        mInstalled = installed;
    }


    // Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dst, int flags) {
        dst.writeLong(mUniqueId);
        dst.writeString(mName);
        dst.writeString(mShortName);
        dst.writeString(mLanguage);
        dst.writeString(mBlobKey);
        dst.writeInt(mSize);
        dst.writeLong(mTimestamp);
        dst.writeByte((byte) (mInstalled ? 1 : 0));
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


    private final long mUniqueId;
    private final String mName;
    private final String mShortName;
    private final String mLanguage;
    private final String mBlobKey;
    private final int mSize;
    private final long mTimestamp;
    private boolean mInstalled;
}
