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
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.Verse;

import java.util.List;

import javax.inject.Inject;

public class SearchResultListAdapter extends BaseAdapter {
    @Inject
    Settings mSettings;

    private final LayoutInflater mInflater;
    private final Resources mResources;
    private List<Verse> mVerses;

    public SearchResultListAdapter(Context context) {
        super();
        App.get(context).getInjectionComponent().inject(this);

        mInflater = LayoutInflater.from(context);
        mResources = context.getResources();
    }

    @Override
    public int getCount() {
        return mVerses == null ? 0 : mVerses.size();
    }

    @Override
    public Verse getItem(int position) {
        return mVerses.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final TextView textView = (TextView) (convertView == null
                ? mInflater.inflate(R.layout.item_search_result, parent, false) : convertView);

        textView.setTextColor(mSettings.getTextColor());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                mResources.getDimension(mSettings.getTextSize().textSize));

        final Verse verse = mVerses.get(position);
        textView.setText(String.format("%s %d:%d\n%s", verse.bookName,
                verse.chapterIndex + 1, verse.verseIndex + 1, verse.verseText));

        return textView;
    }

    public void setVerses(List<Verse> verses) {
        mVerses = verses;
    }
}
