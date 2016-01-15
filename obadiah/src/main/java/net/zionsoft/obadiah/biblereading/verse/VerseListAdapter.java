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

package net.zionsoft.obadiah.biblereading.verse;

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
import net.zionsoft.obadiah.model.domain.VerseWithParallelTranslations;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

class VerseListAdapter extends RecyclerView.Adapter {
    private static final int ITEM_TYPE_VERSE = 0;
    private static final int ITEM_TYPE_VERSE_WITH_PARALLEL_TRANSLATIONS = 1;

    static class VerseViewHolder extends RecyclerView.ViewHolder {
        private static final StringBuilder STRING_BUILDER = new StringBuilder();
        private final Settings settings;
        private final Resources resources;

        @Bind(R.id.text)
        TextView text;

        private VerseViewHolder(View itemView, Settings settings, Resources resources) {
            super(itemView);

            this.settings = settings;
            this.resources = resources;
            ButterKnife.bind(this, itemView);
        }

        private void bind(Verse verse, boolean selected) {
            itemView.setSelected(selected);

            text.setTextColor(settings.getTextColor());
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(settings.getTextSize().textSize));

            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(verse.bookName).append(' ')
                    .append(verse.index.chapter + 1).append(':').append(verse.index.verse + 1).append('\n')
                    .append(verse.verseText);
            text.setText(STRING_BUILDER.toString());
        }
    }

    static class VerseWithParallelTranslationsViewHolder extends RecyclerView.ViewHolder {
        private static final StringBuilder STRING_BUILDER = new StringBuilder();
        private final Settings settings;
        private final Resources resources;

        @Bind(R.id.text)
        TextView text;

        private VerseWithParallelTranslationsViewHolder(View itemView, Settings settings, Resources resources) {
            super(itemView);

            this.settings = settings;
            this.resources = resources;
            ButterKnife.bind(this, itemView);
        }

        private void bind(VerseWithParallelTranslations verse) {
            text.setTextColor(settings.getTextColor());
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(settings.getTextSize().textSize));

            STRING_BUILDER.setLength(0);
            final int size = verse.texts.size();
            for (int i = 0; i < size; ++i) {
                final VerseWithParallelTranslations.Text text = verse.texts.get(i);
                STRING_BUILDER.append(text.translation).append(' ')
                        .append(verse.verseIndex.chapter + 1).append(':').append(verse.verseIndex.verse + 1)
                        .append('\n').append(text.text).append('\n').append('\n');
            }
            text.setText(STRING_BUILDER.substring(0, STRING_BUILDER.length() - 2));
        }
    }

    private final Settings settings;
    private final LayoutInflater inflater;
    private final Resources resources;

    private List<Verse> verses;
    private List<VerseWithParallelTranslations> versesWithParallelTranslations;
    private boolean[] selected;
    private int selectedCount;

    VerseListAdapter(Context context, Settings settings) {
        this.settings = settings;
        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
    }

    @Override
    public int getItemCount() {
        if (verses != null) {
            return verses.size();
        }
        if (versesWithParallelTranslations != null) {
            return versesWithParallelTranslations.size();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (verses != null) {
            return ITEM_TYPE_VERSE;
        }
        if (versesWithParallelTranslations != null) {
            return ITEM_TYPE_VERSE_WITH_PARALLEL_TRANSLATIONS;
        }
        throw new IllegalStateException("Unknown view type for position - " + position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case ITEM_TYPE_VERSE:
                return new VerseViewHolder(inflater.inflate(R.layout.item_verse, parent, false), settings, resources);
            case ITEM_TYPE_VERSE_WITH_PARALLEL_TRANSLATIONS:
                return new VerseWithParallelTranslationsViewHolder(
                        inflater.inflate(R.layout.item_verse_with_parallel_translations, parent, false), settings, resources);
        }
        throw new IllegalStateException("Unknown view type - " + viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof VerseViewHolder) {
            ((VerseViewHolder) holder).bind(verses.get(position), selected[position]);
        } else if (holder instanceof VerseWithParallelTranslationsViewHolder) {
            ((VerseWithParallelTranslationsViewHolder) holder).bind(versesWithParallelTranslations.get(position));
        }
    }

    void setVerses(List<Verse> verses) {
        this.verses = verses;
        this.versesWithParallelTranslations = null;

        final int size = this.verses.size();
        if (selected == null || selected.length < size) {
            selected = new boolean[size];
        }
        deselectVerses();

        notifyDataSetChanged();
    }

    void setVersesWithParallelTranslations(List<VerseWithParallelTranslations> versesWithParallelTranslations) {
        this.verses = null;
        this.versesWithParallelTranslations = versesWithParallelTranslations;

        deselectVerses();

        notifyDataSetChanged();
    }

    void select(int position) {
        if (verses == null) {
            // TODO supports selection for verses with parallel translations
            return;
        }

        selected[position] ^= true;
        if (selected[position]) {
            ++selectedCount;
        } else {
            --selectedCount;
        }
    }

    boolean hasSelectedVerses() {
        return selectedCount > 0;
    }

    List<Verse> getSelectedVerses() {
        final List<Verse> selectedVerses = new ArrayList<>(selectedCount);
        for (int i = 0; i < selected.length; ++i) {
            if (selected[i]) {
                selectedVerses.add(verses.get(i));
            }
        }
        return selectedVerses;
    }

    void deselectVerses() {
        if (selected != null) {
            for (int i = 0; i < selected.length; ++i) {
                selected[i] = false;
            }
        }
        selectedCount = 0;
    }
}
