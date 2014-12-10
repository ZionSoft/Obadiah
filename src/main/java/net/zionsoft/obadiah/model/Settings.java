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
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import net.zionsoft.obadiah.R;

public class Settings {
    public enum TextSize {
        VERY_SMALL("very_small", R.string.pref_text_size_very_small,
                R.dimen.text_size_very_small, R.dimen.smaller_text_size_very_small),
        SMALL("small", R.string.pref_text_size_small,
                R.dimen.text_size_small, R.dimen.smaller_text_size_small),
        MEDIUM("medium", R.string.pref_text_size_medium,
                R.dimen.text_size_medium, R.dimen.smaller_text_size_medium),
        LARGE("large", R.string.pref_text_size_large,
                R.dimen.text_size_large, R.dimen.smaller_text_size_large),
        VERY_LARGE("very_large", R.string.pref_text_size_very_large,
                R.dimen.text_size_very_large, R.dimen.smaller_text_size_very_large);

        private final String mSettingKey;

        @StringRes
        public final int title;

        @DimenRes
        public final int textSize;

        @DimenRes
        public final int smallerTextSize;

        private TextSize(String settingKey, @StringRes int title,
                         @DimenRes int textSize, @DimenRes int smallerTextSize) {
            mSettingKey = settingKey;
            this.title = title;
            this.textSize = textSize;
            this.smallerTextSize = smallerTextSize;
        }

        private static final TextSize DEFAULT = MEDIUM;

        public static TextSize fromSettingKey(@Nullable String settingKey) {
            for (TextSize textSize : TextSize.values()) {
                if (textSize.mSettingKey.equals(settingKey)) {
                    return textSize;
                }
            }

            return DEFAULT;
        }
    }

    private static final String SETTING_KEY_NIGHT_MODE = "pref_night_mode";
    private static final String SETTING_KEY_SCREEN_ON = "pref_screen_on";
    private static final String SETTING_KEY_TEXT_SIZE = "pref_text_size";

    private static Settings sInstance;

    private final SharedPreferences mSharedPreferences;

    private boolean mNightMode;
    private boolean mScreenOn;
    private TextSize mTextSize;

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
        refresh();
    }

    public static Settings getInstance() {
        return sInstance;
    }

    public void refresh() {
        mNightMode = mSharedPreferences.getBoolean(SETTING_KEY_NIGHT_MODE, false);
        mScreenOn = mSharedPreferences.getBoolean(SETTING_KEY_SCREEN_ON, false);

        mTextSize = TextSize.fromSettingKey(mSharedPreferences.getString(SETTING_KEY_TEXT_SIZE, null));
    }

    public boolean keepScreenOn() {
        return mScreenOn;
    }

    public void setKeepScreenOn(boolean keepScreenOn) {
        mScreenOn = keepScreenOn;
        mSharedPreferences.edit()
                .putBoolean(SETTING_KEY_SCREEN_ON, keepScreenOn)
                .apply();
    }

    public boolean isNightMode() {
        return mNightMode;
    }

    public void setNightMode(boolean isNightMode) {
        mNightMode = isNightMode;
        mSharedPreferences.edit()
                .putBoolean(SETTING_KEY_NIGHT_MODE, isNightMode)
                .apply();
    }

    public int getBackgroundColor() {
        return mNightMode ? Color.BLACK : Color.WHITE;
    }

    public int getTextColor() {
        return mNightMode ? Color.WHITE : Color.BLACK;
    }

    public TextSize getTextSize() {
        return mTextSize;
    }

    public void setTextSize(TextSize textSize) {
        mTextSize = textSize;
        mSharedPreferences.edit()
                .putString(SETTING_KEY_TEXT_SIZE, textSize.mSettingKey)
                .apply();
    }
}
