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

package net.zionsoft.obadiah.biblereading.verse;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.v7.widget.AppCompatImageView;
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
    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private static final StringBuilder STRING_BUILDER = new StringBuilder();
        private static final PorterDuffColorFilter FAVORITE_ON = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
        private static final PorterDuffColorFilter FAVORITE_OFF = new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);

        private final VersePagerPresenter versePagerPresenter;
        private final Resources resources;

        @Bind(R.id.text)
        TextView text;

        @Bind(R.id.favorite)
        AppCompatImageView favorite;

        private Verse.Index verseIndex;
        private boolean isFavorite;

        private ViewHolder(View itemView, VersePagerPresenter versePagerPresenter, Resources resources) {
            super(itemView);

            this.versePagerPresenter = versePagerPresenter;
            this.resources = resources;

            ButterKnife.bind(this, itemView);
            favorite.setOnClickListener(this);
        }

        private void bind(Verse verse, boolean selected, boolean isFavorite) {
            itemView.setEnabled(true);
            itemView.setSelected(selected);

            final Settings settings = versePagerPresenter.getSettings();
            text.setTextColor(settings.getTextColor());
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(settings.getTextSize().textSize));

            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(verse.bookName).append(' ')
                    .append(verse.index.chapter + 1).append(':').append(verse.index.verse + 1).append('\n')
                    .append(verse.verseText);
            text.setText(STRING_BUILDER.toString());

            verseIndex = verse.index;
            setFavoriteIcon(isFavorite);
        }

        private void setFavoriteIcon(boolean isFavorite) {
            favorite.setColorFilter(isFavorite ? FAVORITE_ON : FAVORITE_OFF);
        }

        private void bind(VerseWithParallelTranslations verse, boolean isFavorite) {
            itemView.setEnabled(false);
            itemView.setSelected(false);

            final Settings settings = versePagerPresenter.getSettings();
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

            verseIndex = verse.verseIndex;
            setFavoriteIcon(isFavorite);
        }

        @Override
        public void onClick(View v) {
            if (verseIndex != null && v == favorite) {
                isFavorite = !isFavorite;
                setFavoriteIcon(isFavorite);
                if (isFavorite) {
                    versePagerPresenter.addFavorite(verseIndex);
                } else {
                    versePagerPresenter.removeFavorite(verseIndex);
                }
            }
        }
    }

    private final VersePagerPresenter versePagerPresenter;
    private final LayoutInflater inflater;
    private final Resources resources;

    private List<Verse> verses;
    private List<VerseWithParallelTranslations> versesWithParallelTranslations;
    private boolean[] selected;
    private int selectedCount;

    VerseListAdapter(Context context, VersePagerPresenter versePagerPresenter) {
        this.versePagerPresenter = versePagerPresenter;
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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.item_verse, parent, false), versePagerPresenter, resources);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        // TODO
        if (verses != null) {
            ((ViewHolder) holder).bind(verses.get(position), selected[position], false);
        } else if (versesWithParallelTranslations != null) {
            ((ViewHolder) holder).bind(versesWithParallelTranslations.get(position), false);
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
