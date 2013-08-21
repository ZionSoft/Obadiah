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
import android.widget.BaseAdapter;

abstract class ListBaseAdapter extends BaseAdapter {
    ListBaseAdapter(Context context) {
        super();
        mContext = context;
    }

    @Override
    public int getCount() {
        return (mTexts == null) ? 0 : mTexts.length;
    }

    @Override
    public String getItem(int position) {
        return (mTexts == null) ? null : mTexts[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    void setTexts(String[] texts) {
        mTexts = texts;
        notifyDataSetChanged();
    }

    final Context mContext;
    String[] mTexts;
}
