package net.zionsoft.obadiah;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity
{
    public static String PREF_NIGHTMODE = "pref_nightmode";
    public static String PREF_FONTSIZE = "pref_fontsize";

    public static String PREF_FONTSIZE_VERYSMALL = "verysmall";
    public static String PREF_FONTSIZE_SMALL = "small";
    public static String PREF_FONTSIZE_MEDIUM = "medium";
    public static String PREF_FONTSIZE_LARGE = "large";
    public static String PREF_FONTSIZE_VERYLARGE = "verylarge";
    public static String PREF_FONTSIZE_DEFAULT = PREF_FONTSIZE_MEDIUM;

    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        // noinspection deprecation
        addPreferencesFromResource(R.xml.preferences);
    }
}
