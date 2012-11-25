package net.zionsoft.obadiah;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.preference.PreferenceManager;

public class SettingsManager
{
    public SettingsManager(Context context)
    {
        super();
        m_context = context;
    }

    public void refresh()
    {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(m_context);

        if (sharedPreferences.getBoolean(SettingsActivity.PREF_NIGHTMODE, false)) {
            // night mode
            m_backgroundColor = Color.BLACK;
            m_textColor = Color.WHITE;
        } else {
            // day mode
            m_backgroundColor = Color.WHITE;
            m_textColor = Color.BLACK;
        }

        // text size
        final String fontSize = sharedPreferences.getString(SettingsActivity.PREF_FONTSIZE,
                SettingsActivity.PREF_FONTSIZE_DEFAULT);
        final Resources resources = m_context.getResources();
        if (fontSize.equals(SettingsActivity.PREF_FONTSIZE_VERYSMALL)) {
            m_textSize = resources.getDimension(R.dimen.text_size_verysmall);
            m_smallerTextSize = resources.getDimension(R.dimen.smaller_text_size_verysmall);
        } else if (fontSize.equals(SettingsActivity.PREF_FONTSIZE_SMALL)) {
            m_textSize = resources.getDimension(R.dimen.text_size_small);
            m_smallerTextSize = resources.getDimension(R.dimen.smaller_text_size_small);
        } else if (fontSize.equals(SettingsActivity.PREF_FONTSIZE_LARGE)) {
            m_textSize = resources.getDimension(R.dimen.text_size_large);
            m_smallerTextSize = resources.getDimension(R.dimen.smaller_text_size_large);
        } else if (fontSize.equals(SettingsActivity.PREF_FONTSIZE_VERYLARGE)) {
            m_textSize = resources.getDimension(R.dimen.text_size_verylarge);
            m_smallerTextSize = resources.getDimension(R.dimen.smaller_text_size_verylarge);
        } else {
            m_textSize = resources.getDimension(R.dimen.text_size_medium);
            m_smallerTextSize = resources.getDimension(R.dimen.smaller_text_size_medium);
        }
    }

    public float textSize()
    {
        return m_textSize;
    }

    public float smallerTextSize()
    {
        return m_smallerTextSize;
    }

    public int backgroundColor()
    {
        return m_backgroundColor;
    }

    public int textColor()
    {
        return m_textColor;
    }

    private float m_textSize;
    private float m_smallerTextSize;
    private int m_backgroundColor;
    private int m_textColor;
    private Context m_context;
}
