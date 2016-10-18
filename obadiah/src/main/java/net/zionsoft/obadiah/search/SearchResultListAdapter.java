/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2016 ZionSoft
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
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.VerseSearchResult;

import java.util.List;
import java.util.Locale;

class SearchResultListAdapter extends RecyclerView.Adapter {
    private static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private static final SpannableStringBuilder SPANNABLE_STRING_BUILDER = new SpannableStringBuilder();
        private static final StyleSpan BOLD_SPAN = new StyleSpan(Typeface.BOLD);
        private static final RelativeSizeSpan RELATIVE_SIZE_SPAN = new RelativeSizeSpan(1.2F);
        private static final StringBuilder STRING_BUILDER = new StringBuilder();
        private static final Locale DEFAULT_LOCALE = Locale.getDefault();

        private final SearchPresenter searchPresenter;
        private final Resources resources;
        private final TextView textView;

        private VerseSearchResult verse;

        ViewHolder(View itemView, SearchPresenter searchPresenter, Resources resources) {
            super(itemView);
            this.searchPresenter = searchPresenter;
            this.resources = resources;
            this.textView = (TextView) itemView;
            itemView.setOnClickListener(this);
        }

        void bind(String query, VerseSearchResult verse) {
            final Settings settings = searchPresenter.getSettings();
            textView.setTextColor(settings.getTextColor());
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(settings.getTextSize().textSize));

            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(verse.bookName).append(' ')
                    .append(verse.index.chapter() + 1).append(':').append(verse.index.verse() + 1)
                    .append('\n').append(verse.text);
            final int start = STRING_BUILDER.toString().toLowerCase(DEFAULT_LOCALE)
                    .indexOf(query.toLowerCase(DEFAULT_LOCALE));
            if (start >= 0) {
                final int end = start + query.length();

                SPANNABLE_STRING_BUILDER.clear();
                SPANNABLE_STRING_BUILDER.clearSpans();
                SPANNABLE_STRING_BUILDER.append(STRING_BUILDER);
                SPANNABLE_STRING_BUILDER.setSpan(BOLD_SPAN, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                SPANNABLE_STRING_BUILDER.setSpan(RELATIVE_SIZE_SPAN, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

                textView.setText(SPANNABLE_STRING_BUILDER);
            } else {
                // can this happen?
                textView.setText(STRING_BUILDER.toString());
            }

            this.verse = verse;
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

    private String query;
    private List<VerseSearchResult> verses;

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
        ((ViewHolder) holder).bind(query, verses.get(position));
    }

    @Override
    public int getItemCount() {
        return verses != null ? verses.size() : 0;
    }

    void setVerses(String query, List<VerseSearchResult> verses) {
        this.query = query;
        this.verses = verses;
    }
}
