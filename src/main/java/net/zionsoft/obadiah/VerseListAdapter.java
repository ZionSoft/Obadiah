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
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.zionsoft.obadiah.util.SettingsManager;

class VerseListAdapter extends ListBaseAdapter {
    VerseListAdapter(Context context, SettingsManager settingsManager) {
        super(context);
        mSettingsManager = settingsManager;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout linearLayout;
        if (convertView == null)
            linearLayout = (LinearLayout) View.inflate(mContext, R.layout.text_list_item, null);
        else
            linearLayout = (LinearLayout) convertView;

        TextView textView = (TextView) linearLayout.getChildAt(0);
        textView.setTextColor(mSettingsManager.textColor());
        textView.setText(Integer.toString(position + 1));

        textView = (TextView) linearLayout.getChildAt(1);
        textView.setTextColor(mSettingsManager.textColor());
        if (mSelected[position]) {
            final SpannableString string = new SpannableString(mTexts[position]);
            if (mBackgroundColorSpan == null)
                mBackgroundColorSpan = new BackgroundColorSpan(Color.LTGRAY);
            string.setSpan(mBackgroundColorSpan, 0, mTexts[position].length(),
                    SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
            textView.setText(string);
        } else {
            textView.setText(mTexts[position]);
        }

        return linearLayout;
    }

    void setVerses(String currentBookName, int currentChapter, String[] verses) {
        mCurrentBookName = currentBookName;
        mCurrentChapter = currentChapter;
        mTexts = verses;

        final int length = mTexts.length;
        if (mSelected == null || length > mSelected.length)
            mSelected = new boolean[length];
        for (int i = 0; i < length; ++i)
            mSelected[i] = false;
        mSelectedCount = 0;

        notifyDataSetChanged();
    }


    // verses selection

    boolean hasVerseSelected() {
        return (mSelectedCount > 0);
    }

    String selectedText() {
        if (!hasVerseSelected())
            return null;

        // format: <book name> <chapter index>:<verse index> <verse text>
        final String template = new StringBuilder()
                .append(mCurrentBookName).append(mCurrentChapter + 1)
                .append(":%d %s").toString();
        StringBuilder selected = new StringBuilder();
        for (int i = 0; i < mTexts.length; ++i) {
            if (mSelected[i]) {
                if (selected.length() != 0)
                    selected.append("\n");
                selected.append(String.format(template, i + 1, mTexts[i]));
            }
        }
        return selected.toString();
    }

    void selectVerse(int position) {
        if (position < 0 || position >= mTexts.length)
            return;

        mSelected[position] ^= true;
        if (mSelected[position])
            ++mSelectedCount;
        else
            --mSelectedCount;

        notifyDataSetChanged();
    }

    void deselectVerses() {
        if (hasVerseSelected()) {
            final int length = mSelected.length;
            for (int i = 0; i < length; ++i)
                mSelected[i] = false;
            mSelectedCount = 0;

            notifyDataSetChanged();
        }
    }

    private final SettingsManager mSettingsManager;

    private String mCurrentBookName;
    private int mCurrentChapter;

    private boolean mSelected[];
    private int mSelectedCount;
    private BackgroundColorSpan mBackgroundColorSpan;
}
