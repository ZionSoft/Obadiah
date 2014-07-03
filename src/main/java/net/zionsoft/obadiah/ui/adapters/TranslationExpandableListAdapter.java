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

package net.zionsoft.obadiah.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.TranslationInfo;

import java.util.List;

public class TranslationExpandableListAdapter extends BaseExpandableListAdapter {
    public static final int DOWNLOADED_TRANSLATIONS_GROUP = 0;
    public static final int AVAILABLE_TRANSLATIONS_GROUP = 1;

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final String mCurrentTranslation;

    private final int mTextColor;
    private final float mMediumTextSize;
    private final AbsoluteSizeSpan mMediumSizeSpan;
    private final AbsoluteSizeSpan mSmallSizeSpan;

    private List<TranslationInfo> mDownloadedTranslations;
    private List<TranslationInfo> mAvailableTranslations;
    private SpannableStringBuilder[] mAvailableTranslationTexts;

    public TranslationExpandableListAdapter(Context context, String currentTranslation) {
        super();

        mContext = context;
        mInflater = LayoutInflater.from(context);
        mCurrentTranslation = currentTranslation;

        final Settings settings = Settings.getInstance();
        mTextColor = settings.getTextColor();
        mMediumTextSize = settings.getTextSize();
        mMediumSizeSpan = new AbsoluteSizeSpan((int) mMediumTextSize);
        mSmallSizeSpan = new AbsoluteSizeSpan((int) settings.getSmallerTextSize());
    }

    @Override
    public int getGroupCount() {
        return 2;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (groupPosition == DOWNLOADED_TRANSLATIONS_GROUP)
            return mDownloadedTranslations == null ? 0 : mDownloadedTranslations.size();
        if (groupPosition == AVAILABLE_TRANSLATIONS_GROUP)
            return mAvailableTranslations == null ? 0 : mAvailableTranslations.size();
        return 0;
    }

    @Override
    public Integer getGroup(int groupPosition) {
        return groupPosition;
    }

    @Override
    public TranslationInfo getChild(int groupPosition, int childPosition) {
        if (groupPosition == DOWNLOADED_TRANSLATIONS_GROUP)
            return mDownloadedTranslations == null ? null : mDownloadedTranslations.get(childPosition);
        if (groupPosition == AVAILABLE_TRANSLATIONS_GROUP)
            return mAvailableTranslations == null ? null : mAvailableTranslations.get(childPosition);
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition * 1000 + childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        final TextView textView = (TextView) (convertView == null
                ? mInflater.inflate(R.layout.item_translation_section, parent, false) : convertView);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mMediumTextSize);
        textView.setText(groupPosition == 0 ? R.string.text_downloaded_translations : R.string.text_available_translation);
        return textView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        final TextView textView = (TextView) (convertView == null
                ? mInflater.inflate(R.layout.item_translation, parent, false) : convertView);
        if (groupPosition == DOWNLOADED_TRANSLATIONS_GROUP) {
            final TranslationInfo translationInfo = mDownloadedTranslations.get(childPosition);
            if (translationInfo.shortName.equals(mCurrentTranslation))
                textView.setBackgroundColor(Color.LTGRAY);
            else
                textView.setBackgroundResource(R.drawable.background_item_translation);
            textView.setText(translationInfo.name);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mMediumTextSize);
        } else if (groupPosition == AVAILABLE_TRANSLATIONS_GROUP) {
            textView.setBackgroundResource(R.drawable.background_item_translation);
            textView.setText(mAvailableTranslationTexts[childPosition]);
        }
        textView.setTextColor(mTextColor);
        return textView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public void setTranslations(List<TranslationInfo> downloaded, List<TranslationInfo> available) {
        mDownloadedTranslations = downloaded;
        mAvailableTranslations = available;

        if (available != null) {
            mAvailableTranslationTexts = new SpannableStringBuilder[available.size()];
            int i = 0;
            for (TranslationInfo translation : available) {
                final SpannableStringBuilder text = new SpannableStringBuilder(
                        mContext.getResources().getString(R.string.text_available_translation_info,
                                translation.name, translation.size / 1024)
                );
                text.setSpan(mMediumSizeSpan, 0, translation.name.length(), 0);
                text.setSpan(mSmallSizeSpan, translation.name.length(), text.length(), 0);
                mAvailableTranslationTexts[i++] = text;
            }
        }
    }
}
