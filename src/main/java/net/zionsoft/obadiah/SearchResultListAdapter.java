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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zionsoft.obadiah.util.SettingsManager;

public class SearchResultListAdapter extends ListBaseAdapter {
    public SearchResultListAdapter(Context context, SettingsManager settingsManager) {
        super(context);
        mSettingsManager = settingsManager;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView textView;
        if (convertView == null)
            textView = (TextView) View.inflate(mContext, R.layout.search_result_list_item, null);
        else
            textView = (TextView) convertView;

        textView.setTextColor(mSettingsManager.textColor());
        textView.setText(mTexts[position]);
        return textView;
    }

    private final SettingsManager mSettingsManager;
}
