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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.Verse;

import java.util.ArrayList;
import java.util.List;

class VerseListAdapter extends BaseAdapter {
    private static class ViewTag {
        TextView index;
        TextView text;
    }

    private final LayoutInflater mInflater;
    private final Settings mSettings;
    private List<Verse> mVerses;
    private boolean[] mSelected;
    private int mSelectedCount;

    VerseListAdapter(Context context) {
        super();

        mInflater = LayoutInflater.from(context);
        mSettings = Settings.getInstance();
    }

    @Override
    public int getCount() {
        return mVerses == null ? 0 : mVerses.size();
    }

    @Override
    public Verse getItem(int position) {
        return mVerses == null ? null : mVerses.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final LinearLayout linearLayout;
        final ViewTag viewTag;
        if (convertView == null) {
            linearLayout = (LinearLayout) mInflater.inflate(R.layout.item_text, parent, false);

            viewTag = new ViewTag();
            viewTag.index = (TextView) linearLayout.getChildAt(0);
            viewTag.text = (TextView) linearLayout.getChildAt(1);
            linearLayout.setTag(viewTag);
        } else {
            linearLayout = (LinearLayout) convertView;
            viewTag = (ViewTag) linearLayout.getTag();
        }

        linearLayout.setBackgroundColor(mSelected[position] ? Color.LTGRAY : Color.TRANSPARENT);

        final int textColor = mSettings.getTextColor();
        final float textSize = mSettings.getTextSize();
        viewTag.index.setTextColor(textColor);
        viewTag.index.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        viewTag.index.setText(Integer.toString(position + 1));
        viewTag.text.setTextColor(textColor);
        viewTag.text.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        viewTag.text.setText(mVerses.get(position).verseText);

        return linearLayout;
    }

    void setVerses(List<Verse> verses) {
        mVerses = verses;

        final int size = mVerses.size();
        if (mSelected == null || mSelected.length < size)
            mSelected = new boolean[size];
        deselectVerses();
    }

    void select(int position) {
        mSelected[position] ^= true;
        if (mSelected[position])
            ++mSelectedCount;
        else
            --mSelectedCount;
    }

    boolean hasSelectedVerses() {
        return mSelectedCount > 0;
    }

    List<Verse> getSelectedVerses() {
        final List<Verse> selectedVerses = new ArrayList<Verse>(mSelectedCount);
        int i = 0;
        for (boolean selected : mSelected) {
            if (selected)
                selectedVerses.add(mVerses.get(i));
            ++i;
        }
        return selectedVerses;
    }

    void deselectVerses() {
        for (int i = 0; i < mSelected.length; ++i)
            mSelected[i] = false;
        mSelectedCount = 0;
    }
}
