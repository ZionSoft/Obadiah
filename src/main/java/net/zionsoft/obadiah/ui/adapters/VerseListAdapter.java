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
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;

class VerseListAdapter extends BaseAdapter {
    private static class ViewTag {
        TextView index;
        TextView text;
    }

    private final Context mContext;
    private final Settings mSettings;
    private String[] mVerses;

    VerseListAdapter(Context context) {
        super();

        mContext = context;
        mSettings = Settings.getInstance();
    }

    @Override
    public int getCount() {
        return mVerses == null ? 0 : mVerses.length;
    }

    @Override
    public String getItem(int position) {
        return mVerses == null ? null : mVerses[position];
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
            linearLayout = (LinearLayout) View.inflate(mContext, R.layout.item_text, null);

            viewTag = new ViewTag();
            viewTag.index = (TextView) linearLayout.getChildAt(0);
            viewTag.text = (TextView) linearLayout.getChildAt(1);
            linearLayout.setTag(viewTag);
        } else {
            linearLayout = (LinearLayout) convertView;
            viewTag = (ViewTag) linearLayout.getTag();
        }

        final int textColor = mSettings.getTextColor();
        final float textSize = mSettings.getTextSize();
        viewTag.index.setTextColor(textColor);
        viewTag.index.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        viewTag.index.setText(Integer.toString(position + 1));
        viewTag.text.setTextColor(textColor);
        viewTag.text.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        viewTag.text.setText(mVerses[position]);

        return linearLayout;
    }

    void setVerses(String[] verses) {
        mVerses = verses;
    }
}
