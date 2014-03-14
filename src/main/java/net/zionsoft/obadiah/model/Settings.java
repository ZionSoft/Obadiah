/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2014 ZionSoft
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

package net.zionsoft.obadiah.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.preference.PreferenceManager;

import net.zionsoft.obadiah.R;

public class Settings {
    private static final String SETTING_KEY_NIGHT_MODE = "pref_night_mode";
    private static final String SETTING_KEY_TEXT_SIZE = "pref_text_size";

    private static final String TEXT_SIZE_VERY_SMALL = "very_small";
    private static final String TEXT_SIZE_SMALL = "small";
    private static final String TEXT_SIZE_MEDIUM = "medium";
    private static final String TEXT_SIZE_LARGE = "large";
    private static final String TEXT_SIZE_VERY_LARGE = "very_large";

    private static Settings sInstance;

    private final SharedPreferences mSharedPreferences;
    private final Resources mResources;

    private boolean mNightMode;
    private float mTextSize;
    private float mSmallerTextSize;

    public static void initialize(Context context) {
        if (sInstance == null) {
            synchronized (Settings.class) {
                if (sInstance == null)
                    sInstance = new Settings(context.getApplicationContext());
            }
        }
    }

    private Settings(Context context) {
        super();

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mResources = context.getResources();
    }

    public static Settings getInstance() {
        return sInstance;
    }

    public void refresh() {
        mNightMode = mSharedPreferences.getBoolean(SETTING_KEY_NIGHT_MODE, false);

        final String textSize = mSharedPreferences.getString(
                SETTING_KEY_TEXT_SIZE, TEXT_SIZE_MEDIUM);
        if (textSize.equals(TEXT_SIZE_VERY_SMALL)) {
            mTextSize = mResources.getDimension(R.dimen.text_size_very_small);
            mSmallerTextSize = mResources.getDimension(R.dimen.smaller_text_size_very_small);
        } else if (textSize.equals(TEXT_SIZE_SMALL)) {
            mTextSize = mResources.getDimension(R.dimen.text_size_small);
            mSmallerTextSize = mResources.getDimension(R.dimen.smaller_text_size_small);
        } else if (textSize.equals(TEXT_SIZE_LARGE)) {
            mTextSize = mResources.getDimension(R.dimen.text_size_large);
            mSmallerTextSize = mResources.getDimension(R.dimen.smaller_text_size_large);
        } else if (textSize.equals(TEXT_SIZE_VERY_LARGE)) {
            mTextSize = mResources.getDimension(R.dimen.text_size_very_large);
            mSmallerTextSize = mResources.getDimension(R.dimen.smaller_text_size_very_large);
        } else {
            mTextSize = mResources.getDimension(R.dimen.text_size_medium);
            mSmallerTextSize = mResources.getDimension(R.dimen.smaller_text_size_medium);
        }
    }

    public int getBackgroundColor() {
        return mNightMode ? Color.BLACK : Color.WHITE;
    }

    public int getTextColor() {
        return mNightMode ? Color.WHITE : Color.BLACK;
    }

    public float getTextSize() {
        return mTextSize;
    }

    public float getSmallerTextSize() {
        return mSmallerTextSize;
    }
}
