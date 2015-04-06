/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2015 ZionSoft
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
import android.widget.LinearLayout;
import android.widget.TextView;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.Verse;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class VerseListAdapter extends BaseAdapter {
    private static class ViewTag {
        TextView index;
        TextView text;
    }

    @Inject
    Settings settings;

    private final LayoutInflater inflater;
    private final Resources resources;

    private List<Verse> verses;
    private boolean[] selected;
    private int selectedCount;

    public VerseListAdapter(Context context) {
        App.get(context).getInjectionComponent().inject(this);

        inflater = LayoutInflater.from(context);
        resources = context.getResources();
    }

    @Override
    public int getCount() {
        return verses == null ? 0 : verses.size();
    }

    @Override
    public Verse getItem(int position) {
        return verses == null ? null : verses.get(position);
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
            linearLayout = (LinearLayout) inflater.inflate(R.layout.item_text, parent, false);

            viewTag = new ViewTag();
            viewTag.index = (TextView) linearLayout.getChildAt(0);
            viewTag.text = (TextView) linearLayout.getChildAt(1);
            linearLayout.setTag(viewTag);
        } else {
            linearLayout = (LinearLayout) convertView;
            viewTag = (ViewTag) linearLayout.getTag();
        }

        if (selected[position]) {
            linearLayout.setBackgroundColor(resources.getColor(R.color.blue_semi_transparent));
        } else {
            linearLayout.setBackgroundResource(R.drawable.background_text);
        }

        final int textColor = settings.getTextColor();
        final float textSize = resources.getDimension(settings.getTextSize().textSize);
        viewTag.index.setTextColor(textColor);
        viewTag.index.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        final int count = getCount();
        if (count < 10) {
            viewTag.index.setText(Integer.toString(position + 1));
        } else if (count < 100) {
            viewTag.index.setText(String.format("%2d", position + 1));
        } else {
            viewTag.index.setText(String.format("%3d", position + 1));
        }

        viewTag.text.setTextColor(textColor);
        viewTag.text.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        viewTag.text.setText(verses.get(position).verseText);

        return linearLayout;
    }

    void setVerses(List<Verse> verses) {
        this.verses = verses;

        final int size = this.verses.size();
        if (selected == null || selected.length < size)
            selected = new boolean[size];
        deselectVerses();
    }

    void select(int position) {
        selected[position] ^= true;
        if (selected[position])
            ++selectedCount;
        else
            --selectedCount;
    }

    boolean hasSelectedVerses() {
        return selectedCount > 0;
    }

    List<Verse> getSelectedVerses() {
        final List<Verse> selectedVerses = new ArrayList<Verse>(selectedCount);
        int i = 0;
        for (boolean selected : this.selected) {
            if (selected)
                selectedVerses.add(verses.get(i));
            ++i;
        }
        return selectedVerses;
    }

    void deselectVerses() {
        for (int i = 0; i < selected.length; ++i)
            selected[i] = false;
        selectedCount = 0;
    }
}
