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
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.util.SettingsManager;

import java.util.List;

class TranslationSelectionListAdapter extends BaseAdapter {
    TranslationSelectionListAdapter(Context context, SettingsManager settingsManager) {
        super();
        mContext = context;
        mSettingsManager = settingsManager;
    }

    @Override
    public int getCount() {
        return (mTranslations == null) ? 0 : mTranslations.size();
    }

    @Override
    public TranslationInfo getItem(int position) {
        return (mTranslations == null) ? null : mTranslations.get(position);
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
                    R.layout.translation_selection_list_item, null);
        } else {
            textView = (TextView) convertView;
        }

        textView.setTextColor(mSettingsManager.textColor());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSettingsManager.textSize());

        final TranslationInfo translationInfo = mTranslations.get(position);
        textView.setText(translationInfo.name());
        if (translationInfo.shortName().equals(mSelectedTranslationShortName)) {
            textView.setTypeface(null, Typeface.BOLD);
            textView.setBackgroundResource(R.drawable.list_item_background_selected);
        } else {
            textView.setTypeface(null, Typeface.NORMAL);
            textView.setBackgroundResource(R.drawable.list_item_background);
        }
        return textView;
    }

    void setTranslations(List<TranslationInfo> translations) {
        mTranslations = translations;
        notifyDataSetChanged();
    }

    void setSelectedTranslation(String selectedTranslationShortName) {
        mSelectedTranslationShortName = selectedTranslationShortName;
        notifyDataSetChanged();
    }

    private final Context mContext;
    private final SettingsManager mSettingsManager;

    private List<TranslationInfo> mTranslations;
    private String mSelectedTranslationShortName;
}
