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

package net.zionsoft.obadiah.search;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Verse;

import java.util.List;

class SearchResultListAdapter extends RecyclerView.Adapter {
    private static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final SearchPresenter searchPresenter;
        private Verse verse;

        private ViewHolder(SearchPresenter searchPresenter, View itemView) {
            super(itemView);
            this.searchPresenter = searchPresenter;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (verse != null) {
                searchPresenter.saveReadingProgress(verse.index);
                searchPresenter.openBibleReadingActivity();
            }
        }
    }

    private final SearchPresenter searchPresenter;
    private final LayoutInflater inflater;
    private final Resources resources;
    private final Settings settings;

    private List<Verse> verses;

    SearchResultListAdapter(Context context, SearchPresenter searchPresenter, Settings settings) {
        this.searchPresenter = searchPresenter;
        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
        this.settings = settings;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(searchPresenter, inflater.inflate(R.layout.item_search_result, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final TextView textView = (TextView) holder.itemView;
        textView.setTextColor(settings.getTextColor());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(settings.getTextSize().textSize));

        final Verse verse = getVerse(position);
        textView.setText(String.format("%s %d:%d\n%s", verse.bookName,
                verse.index.chapter + 1, verse.index.verse + 1, verse.verseText));
        ((ViewHolder) holder).verse = verse;
    }

    @Override
    public int getItemCount() {
        return verses != null ? verses.size() : 0;
    }

    void setVerses(List<Verse> verses) {
        this.verses = verses;
    }

    Verse getVerse(int position) {
        return verses.get(position);
    }
}
