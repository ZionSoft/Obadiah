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

public class SettingsManager {
    public SettingsManager(Context context) {
        super();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mResources = context.getResources();
    }

    public boolean refresh() {
        boolean settingsChanged = false;

        // day / night mode
        boolean nightMode = mSharedPreferences.getBoolean(SETTING_KEY_NIGHT_MODE, false);
        if (nightMode) {
            if (mMode != NIGHT_MODE) {
                mMode = NIGHT_MODE;
                settingsChanged = true;
            }
        } else {
            if (mMode != DAY_MODE) {
                mMode = DAY_MODE;
                settingsChanged = true;
            }
        }

        // text size
        final String textSize
                = mSharedPreferences.getString(SETTING_KEY_TEXT_SIZE, TEXT_SIZE_MEDIUM);
        if (!textSize.equals(mTextSizeString)) {
            mTextSizeString = textSize;
            if (mTextSizeString.equals(TEXT_SIZE_VERY_SMALL)) {
                mTextSize = mResources.getDimension(R.dimen.text_size_very_small);
                mSmallerTextSize = mResources.getDimension(R.dimen.smaller_text_size_very_small);
            } else if (mTextSizeString.equals(TEXT_SIZE_SMALL)) {
                mTextSize = mResources.getDimension(R.dimen.text_size_small);
                mSmallerTextSize = mResources.getDimension(R.dimen.smaller_text_size_small);
            } else if (mTextSizeString.equals(TEXT_SIZE_LARGE)) {
                mTextSize = mResources.getDimension(R.dimen.text_size_large);
                mSmallerTextSize = mResources.getDimension(R.dimen.smaller_text_size_large);
            } else if (mTextSizeString.equals(TEXT_SIZE_VERY_LARGE)) {
                mTextSize = mResources.getDimension(R.dimen.text_size_very_large);
                mSmallerTextSize = mResources.getDimension(R.dimen.smaller_text_size_very_large);
            } else {
                mTextSize = mResources.getDimension(R.dimen.text_size_medium);
                mSmallerTextSize = mResources.getDimension(R.dimen.smaller_text_size_medium);
            }
            settingsChanged = true;
        }

        return settingsChanged;
    }

    public int backgroundColor() {
        return mMode == NIGHT_MODE ? Color.BLACK : Color.WHITE;
    }

    public int textColor() {
        return mMode == NIGHT_MODE ? Color.WHITE : Color.BLACK;
    }

    public float textSize() {
        return mTextSize;
    }

    public float smallerTextSize() {
        return mSmallerTextSize;
    }

    private static final String SETTING_KEY_NIGHT_MODE = "pref_night_mode";
    private static final String SETTING_KEY_TEXT_SIZE = "pref_text_size";

    private static final int UNKNOWN_MODE = 0;
    private static final int DAY_MODE = 1;
    private static final int NIGHT_MODE = 2;

    private static final String TEXT_SIZE_VERY_SMALL = "very_small";
    private static final String TEXT_SIZE_SMALL = "small";
    private static final String TEXT_SIZE_MEDIUM = "medium";
    private static final String TEXT_SIZE_LARGE = "large";
    private static final String TEXT_SIZE_VERY_LARGE = "very_large";

    private final SharedPreferences mSharedPreferences;
    private final Resources mResources;
    private int mMode = UNKNOWN_MODE;
    private float mTextSize;
    private float mSmallerTextSize;
    private String mTextSizeString;
}
