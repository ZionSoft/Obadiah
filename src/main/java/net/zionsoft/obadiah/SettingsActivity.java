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

package net.zionsoft.obadiah;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity {
    public static final String PREF_NIGHTMODE = "pref_nightmode";
    public static final String PREF_FONTSIZE = "pref_fontsize";

    public static final String PREF_FONTSIZE_VERYSMALL = "verysmall";
    public static final String PREF_FONTSIZE_SMALL = "small";
    public static final String PREF_FONTSIZE_MEDIUM = "medium";
    public static final String PREF_FONTSIZE_LARGE = "large";
    public static final String PREF_FONTSIZE_VERYLARGE = "verylarge";
    public static final String PREF_FONTSIZE_DEFAULT = PREF_FONTSIZE_MEDIUM;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // noinspection deprecation
        addPreferencesFromResource(R.xml.preferences);
    }
}
