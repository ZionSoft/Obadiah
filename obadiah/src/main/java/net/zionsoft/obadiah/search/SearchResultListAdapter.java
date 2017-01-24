/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2017 ZionSoft
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

import java.util.ArrayList;
import java.util.List;

class SearchResultListAdapter extends RecyclerView.Adapter {
    private static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final SearchPresenter searchPresenter;
        private final Resources resources;
        private final TextView textView;

        private SearchedVerse verse;

        ViewHolder(View itemView, SearchPresenter searchPresenter, Resources resources) {
            super(itemView);
            this.searchPresenter = searchPresenter;
            this.resources = resources;
            this.textView = (TextView) itemView;
            itemView.setOnClickListener(this);
        }

        void bind(SearchedVerse verse) {
            this.verse = verse;

            final Settings settings = searchPresenter.getSettings();
            textView.setTextColor(settings.getTextColor());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(settings.getTextSize().textSize));
            textView.setText(verse.getTextForDisplay());
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
    private final List<SearchedVerse> verses = new ArrayList<>();

    SearchResultListAdapter(Context context, SearchPresenter searchPresenter) {
        this.searchPresenter = searchPresenter;
        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.item_search_result, parent, false),
                searchPresenter, resources);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder).bind(verses.get(position));
    }

    @Override
    public int getItemCount() {
        return verses.size();
    }

    void clear() {
        verses.clear();
        notifyDataSetChanged();
    }

    void addVerses(List<SearchedVerse> verses) {
        final int originalCount = this.verses.size();
        this.verses.addAll(verses);
        notifyItemRangeInserted(originalCount, verses.size());
    }
}
