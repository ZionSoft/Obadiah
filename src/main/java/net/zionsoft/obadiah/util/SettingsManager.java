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

package net.zionsoft.obadiah.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.preference.PreferenceManager;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.SettingsActivity;

public class SettingsManager {
    public SettingsManager(Context context) {
        super();
        mContext = context;
    }

    public void refresh() {
        final SharedPreferences sharedPreferences
                = PreferenceManager.getDefaultSharedPreferences(mContext);

        // night / day mode
        if (sharedPreferences.getBoolean(SettingsActivity.PREF_NIGHTMODE, false)) {
            mBackgroundColor = Color.BLACK;
            mTextColor = Color.WHITE;
        } else {
            mBackgroundColor = Color.WHITE;
            mTextColor = Color.BLACK;
        }

        // text size
        final String fontSize = sharedPreferences.getString(SettingsActivity.PREF_FONTSIZE,
                SettingsActivity.PREF_FONTSIZE_DEFAULT);
        final Resources resources = mContext.getResources();
        if (fontSize.equals(SettingsActivity.PREF_FONTSIZE_VERYSMALL)) {
            mTextSize = resources.getDimension(R.dimen.text_size_verysmall);
            mSmallerTextSize = resources.getDimension(R.dimen.smaller_text_size_verysmall);
        } else if (fontSize.equals(SettingsActivity.PREF_FONTSIZE_SMALL)) {
            mTextSize = resources.getDimension(R.dimen.text_size_small);
            mSmallerTextSize = resources.getDimension(R.dimen.smaller_text_size_small);
        } else if (fontSize.equals(SettingsActivity.PREF_FONTSIZE_LARGE)) {
            mTextSize = resources.getDimension(R.dimen.text_size_large);
            mSmallerTextSize = resources.getDimension(R.dimen.smaller_text_size_large);
        } else if (fontSize.equals(SettingsActivity.PREF_FONTSIZE_VERYLARGE)) {
            mTextSize = resources.getDimension(R.dimen.text_size_verylarge);
            mSmallerTextSize = resources.getDimension(R.dimen.smaller_text_size_verylarge);
        } else {
            mTextSize = resources.getDimension(R.dimen.text_size_medium);
            mSmallerTextSize = resources.getDimension(R.dimen.smaller_text_size_medium);
        }
    }

    public float textSize() {
        return mTextSize;
    }

    public float smallerTextSize() {
        return mSmallerTextSize;
    }

    public int backgroundColor() {
        return mBackgroundColor;
    }

    public int textColor() {
        return mTextColor;
    }

    private final Context mContext;
    private float mTextSize;
    private float mSmallerTextSize;
    private int mBackgroundColor;
    private int mTextColor;
}
