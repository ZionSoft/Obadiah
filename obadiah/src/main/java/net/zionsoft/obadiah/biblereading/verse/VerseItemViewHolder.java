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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.utils.TextFormatter;

import butterknife.Bind;
import butterknife.ButterKnife;

class VerseItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, TextWatcher {
    private static final StringBuilder STRING_BUILDER = new StringBuilder();
    static final PorterDuffColorFilter ON = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
    static final PorterDuffColorFilter OFF = new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);

    private final VersePagerPresenter versePagerPresenter;
    private final Resources resources;

    @Bind(R.id.index)
    TextView index;

    @Bind(R.id.text)
    TextView text;

    @Bind(R.id.note)
    TextView note;

    @Bind(R.id.bookmarkIcon)
    AppCompatImageView bookmarkIcon;

    @Bind(R.id.noteIcon)
    AppCompatImageView noteIcon;

    @Bind(R.id.divider)
    View divider;

    private VerseIndex verseIndex;
    private boolean isBookmarked;
    private boolean isExpanded;

    VerseItemViewHolder(LayoutInflater inflater, ViewGroup parent,
                        VersePagerPresenter versePagerPresenter, Resources resources) {
        super(inflater.inflate(R.layout.item_verse, parent, false));

        this.versePagerPresenter = versePagerPresenter;
        this.resources = resources;

        ButterKnife.bind(this, itemView);
        bookmarkIcon.setOnClickListener(this);
        noteIcon.setOnClickListener(this);
    }

    void bind(Verse verse, int totalVerses, boolean selected) {
        itemView.setSelected(selected);

        if (verse.parallel.size() == 0) {
            divider.setVisibility(View.GONE);

            index.setVisibility(View.VISIBLE);
            final Settings settings = versePagerPresenter.getSettings();
            final int textColor = settings.getTextColor();
            final float textSize = resources.getDimension(settings.getTextSize().textSize);
            index.setTextColor(textColor);
            index.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            if (totalVerses < 10) {
                index.setText(Integer.toString(verse.verseIndex.verse() + 1));
            } else if (totalVerses < 100) {
                index.setText(TextFormatter.format("%2d", verse.verseIndex.verse() + 1));
            } else {
                index.setText(TextFormatter.format("%3d", verse.verseIndex.verse() + 1));
            }

            text.setTextColor(textColor);
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            text.setText(verse.text.text);
        } else {
            setVerse(verse);
        }

        bookmarkIcon.setVisibility(View.GONE);
        noteIcon.setVisibility(View.GONE);
        setExpanded(false);
    }

    private void setVerse(Verse verse) {
        final Settings settings = versePagerPresenter.getSettings();
        text.setTextColor(settings.getTextColor());
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(settings.getTextSize().textSize));

        STRING_BUILDER.setLength(0);
        if (verse.parallel.size() == 0) {
            STRING_BUILDER.append(verse.text.bookName).append(' ')
                    .append(verse.verseIndex.chapter() + 1).append(':').append(verse.verseIndex.verse() + 1).append('\n')
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

        divider.setVisibility(View.VISIBLE);
        index.setVisibility(View.GONE);
    }

    private static void buildVerse(StringBuilder sb, VerseIndex verseIndex, Verse.Text text) {
        sb.append(text.translation).append(' ')
                .append(verseIndex.chapter() + 1).append(':').append(verseIndex.verse() + 1)
                .append('\n').append(text.text).append('\n').append('\n');
    }

    void bind(Verse verse, boolean selected, boolean isBookmarked, String note, boolean expanded) {
        itemView.setSelected(selected);

        setVerse(verse);

        bookmarkIcon.setVisibility(View.VISIBLE);
        setBookmark(isBookmarked);

        final Settings settings = versePagerPresenter.getSettings();
        noteIcon.setVisibility(View.VISIBLE);
        this.note.setTextColor(settings.getTextColor());
        this.note.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(settings.getTextSize().smallerTextSize));
        setNote(note, expanded);
        setExpanded(expanded);
    }

    void setBookmark(boolean isBookmarked) {
        this.isBookmarked = isBookmarked;
        bookmarkIcon.setColorFilter(isBookmarked ? ON : OFF);
    }

    private void setNote(String note, boolean expanded) {
        this.note.removeTextChangedListener(this);
        this.note.setText(note);
        this.note.addTextChangedListener(this);

        noteIcon.setColorFilter(TextUtils.isEmpty(note) ? OFF : ON);
        noteIcon.setImageResource(expanded ? R.drawable.ic_arrow_up : R.drawable.ic_note);
    }

    private void setExpanded(boolean expanded) {
        isExpanded = expanded;
        note.setVisibility(expanded ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (verseIndex != null) {
            if (v == bookmarkIcon) {
                if (isBookmarked) {
                    versePagerPresenter.removeBookmark(verseIndex);
                } else {
                    versePagerPresenter.addBookmark(verseIndex);
                }
            } else if (v == noteIcon) {
                if (isExpanded) {
                    versePagerPresenter.hideNote(verseIndex);
                } else {
                    versePagerPresenter.showNote(verseIndex);
                }
                setExpanded(!isExpanded);
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // do nothing
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // do nothing
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (verseIndex != null) {
            final String note = s.toString();
            if (TextUtils.isEmpty(note)) {
                versePagerPresenter.removeNote(verseIndex);
            } else {
                versePagerPresenter.updateNote(verseIndex, s.toString());
            }
        }
    }
}
