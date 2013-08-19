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

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.util.SettingsManager;

import java.util.ArrayList;
import java.util.List;

public class TranslationDownloadListAdapter extends BaseAdapter {
    public TranslationDownloadListAdapter(Context context, SettingsManager settingsManager) {
        super();
        mContext = context;
        mSettingsManager = settingsManager;

        Resources resources = context.getResources();
        mMediumSizeSpan
                = new AbsoluteSizeSpan(resources.getDimensionPixelSize(R.dimen.text_size_medium));
        mSmallSizeSpan
                = new AbsoluteSizeSpan(resources.getDimensionPixelSize(R.dimen.text_size_small));
    }

    @Override
    public int getCount() {
        return (m_texts == null) ? 0 : m_texts.size();
    }

    @Override
    public SpannableStringBuilder getItem(int position) {
        return (m_texts == null) ? null : m_texts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null) {
            textView = (TextView) View.inflate(mContext,
                    R.layout.translation_download_list_item, null);
        } else {
            textView = (TextView) convertView;
        }
        textView.setTextColor(mSettingsManager.textColor());
        textView.setText(m_texts.get(position));
        return textView;
    }

    public void setTranslations(List<TranslationInfo> translations) {
        if (translations == null) {
            m_texts = null;
        } else {
            m_texts = new ArrayList<SpannableStringBuilder>(translations.size());
            for (TranslationInfo translation : translations) {
                String string
                        = mContext.getResources().getString(R.string.text_available_translation_info,
                        translation.name, translation.size / 1024);
                SpannableStringBuilder text = new SpannableStringBuilder(string);
                text.setSpan(mMediumSizeSpan, 0, translation.name.length(), 0);
                text.setSpan(mSmallSizeSpan, translation.name.length(), text.length(), 0);
                m_texts.add(text);
            }
        }
        notifyDataSetChanged();
    }

    private final Context mContext;
    private final SettingsManager mSettingsManager;

    private AbsoluteSizeSpan mMediumSizeSpan;
    private AbsoluteSizeSpan mSmallSizeSpan;
    private List<SpannableStringBuilder> m_texts;
}
