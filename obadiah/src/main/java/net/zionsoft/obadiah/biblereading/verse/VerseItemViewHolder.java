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
import net.zionsoft.obadiah.model.domain.VerseIndex;

import butterknife.Bind;
import butterknife.ButterKnife;

class VerseItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private static final StringBuilder STRING_BUILDER = new StringBuilder();
    private static final PorterDuffColorFilter BOOKMARK_ON = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
    private static final PorterDuffColorFilter BOOKMARK_OFF = new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);

    private final VersePagerPresenter versePagerPresenter;
    private final Resources resources;

    @Bind(R.id.text)
    TextView text;

    @Bind(R.id.bookmark)
    AppCompatImageView bookmark;

    private VerseIndex verseIndex;
    private boolean isBookmarked;

    VerseItemViewHolder(LayoutInflater inflater, ViewGroup parent,
                        VersePagerPresenter versePagerPresenter, Resources resources) {
        super(inflater.inflate(R.layout.item_verse, parent, false));

        this.versePagerPresenter = versePagerPresenter;
        this.resources = resources;

        ButterKnife.bind(this, itemView);
        bookmark.setOnClickListener(this);
    }

    void bind(Verse verse, boolean selected, boolean isBookmarked) {
        itemView.setSelected(selected);

        final Settings settings = versePagerPresenter.getSettings();
        text.setTextColor(settings.getTextColor());
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(settings.getTextSize().textSize));

        STRING_BUILDER.setLength(0);
        if (verse.parallel.size() == 0) {
            STRING_BUILDER.append(verse.text.bookName).append(' ')
                    .append(verse.verseIndex.chapter + 1).append(':').append(verse.verseIndex.verse + 1).append('\n')
                    .append(verse.text.text);
            text.setText(STRING_BUILDER.toString());
        } else {
            buildVerse(STRING_BUILDER, verse.verseIndex, verse.text);
            final int size = verse.parallel.size();
            for (int i = 0; i < size; ++i) {
                final Verse.Text text = verse.parallel.get(i);
                buildVerse(STRING_BUILDER, verse.verseIndex, text);
            }
            text.setText(STRING_BUILDER.substring(0, STRING_BUILDER.length() - 2));
        }

        verseIndex = verse.verseIndex;

        setBookmark(isBookmarked);
    }

    private static void buildVerse(StringBuilder sb, VerseIndex verseIndex, Verse.Text text) {
        sb.append(text.translation).append(' ')
                .append(verseIndex.chapter + 1).append(':').append(verseIndex.verse + 1)
                .append('\n').append(text.text).append('\n').append('\n');
    }

    void setBookmark(boolean isBookmarked) {
        this.isBookmarked = isBookmarked;
        bookmark.setColorFilter(isBookmarked ? BOOKMARK_ON : BOOKMARK_OFF);
    }

    @Override
    public void onClick(View v) {
        if (verseIndex != null && v == bookmark) {
            if (isBookmarked) {
                versePagerPresenter.removeBookmark(verseIndex);
            } else {
                versePagerPresenter.addBookmark(verseIndex);
            }
        }
    }
}
