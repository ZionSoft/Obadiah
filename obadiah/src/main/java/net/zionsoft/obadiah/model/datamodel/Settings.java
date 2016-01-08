/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2015 ZionSoft
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

package net.zionsoft.obadiah.model.datamodel;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import net.zionsoft.obadiah.R;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
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

        private final String settingKey;

        @StringRes
        public final int title;

        @DimenRes
        public final int textSize;

        @DimenRes
        public final int smallerTextSize;

        TextSize(String settingKey, @StringRes int title,
                 @DimenRes int textSize, @DimenRes int smallerTextSize) {
            this.settingKey = settingKey;
            this.title = title;
            this.textSize = textSize;
            this.smallerTextSize = smallerTextSize;
        }

        private static final TextSize DEFAULT = MEDIUM;

        private static TextSize fromSettingKey(@Nullable String settingKey) {
            for (TextSize textSize : TextSize.values()) {
                if (textSize.settingKey.equals(settingKey)) {
                    return textSize;
                }
            }

            return DEFAULT;
        }
    }

    private static final String SETTING_KEY_NIGHT_MODE = "pref_night_mode";
    private static final String SETTING_KEY_SCREEN_ON = "pref_screen_on";
    private static final String SETTING_KEY_TEXT_SIZE = "pref_text_size";

    private final SharedPreferences sharedPreferences;

    private boolean nightMode;
    private boolean screenOn;
    private TextSize textSize;

    @Inject
    public Settings(Context context) {
        super();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        nightMode = sharedPreferences.getBoolean(SETTING_KEY_NIGHT_MODE, false);
        screenOn = sharedPreferences.getBoolean(SETTING_KEY_SCREEN_ON, false);
        textSize = TextSize.fromSettingKey(sharedPreferences.getString(SETTING_KEY_TEXT_SIZE, null));
    }

    public boolean keepScreenOn() {
        return screenOn;
    }

    public void setKeepScreenOn(boolean keepScreenOn) {
        screenOn = keepScreenOn;
        sharedPreferences.edit()
                .putBoolean(SETTING_KEY_SCREEN_ON, keepScreenOn)
                .apply();
    }

    public boolean isNightMode() {
        return nightMode;
    }

    public void setNightMode(boolean isNightMode) {
        nightMode = isNightMode;
        sharedPreferences.edit()
                .putBoolean(SETTING_KEY_NIGHT_MODE, isNightMode)
                .apply();
    }

    @ColorInt
    public int getBackgroundColor() {
        return nightMode ? Color.BLACK : Color.WHITE;
    }

    @ColorInt
    public int getTextColor() {
        return nightMode ? Color.WHITE : Color.BLACK;
    }

    public TextSize getTextSize() {
        return textSize;
    }

    public void setTextSize(TextSize textSize) {
        this.textSize = textSize;
        sharedPreferences.edit()
                .putString(SETTING_KEY_TEXT_SIZE, textSize.settingKey)
                .apply();
    }
}
