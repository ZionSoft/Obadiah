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

package net.zionsoft.obadiah.biblereading;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.domain.Verse;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

class VerseListAdapter extends RecyclerView.Adapter {
    static class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.index)
        TextView index;

        @Bind(R.id.text)
        TextView text;

        private ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    private final Settings settings;
    private final LayoutInflater inflater;
    private final Resources resources;

    private List<Verse> verses;
    private boolean[] selected;
    private int selectedCount;

    VerseListAdapter(Context context, Settings settings) {
        this.settings = settings;
        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
    }

    @Override
    public int getItemCount() {
        return verses != null ? verses.size() : 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.item_text, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final ViewHolder viewHolder = (ViewHolder) holder;
        if (selected[position]) {
            viewHolder.itemView.setBackgroundColor(resources.getColor(R.color.blue_semi_transparent));
        } else {
            viewHolder.itemView.setBackgroundResource(R.drawable.background_text);
        }

        final int textColor = settings.getTextColor();
        final float textSize = resources.getDimension(settings.getTextSize().textSize);
        viewHolder.index.setTextColor(textColor);
        viewHolder.index.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        final int count = getItemCount();
        if (count < 10) {
            viewHolder.index.setText(Integer.toString(position + 1));
        } else if (count < 100) {
            viewHolder.index.setText(String.format("%2d", position + 1));
        } else {
            viewHolder.index.setText(String.format("%3d", position + 1));
        }

        viewHolder.text.setTextColor(textColor);
        viewHolder.text.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        viewHolder.text.setText(verses.get(position).verseText);
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
        final List<Verse> selectedVerses = new ArrayList<>(selectedCount);
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
