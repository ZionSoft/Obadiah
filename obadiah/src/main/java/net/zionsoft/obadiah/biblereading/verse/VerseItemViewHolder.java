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

package net.zionsoft.obadiah.biblereading.verse;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
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

import butterknife.BindView;
import butterknife.ButterKnife;

class VerseItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
        View.OnLongClickListener, TextWatcher, VerseDetailDialogFragment.Listener {
    private static final String VERSE_DETAIL_FRAGMENT_TAG = "net.zionsoft.net.VERSE_DETAIL_FRAGMENT_TAG";

    private static final RelativeSizeSpan PARALLEL_VERSE_SIZE_SPAN = new RelativeSizeSpan(0.95F);
    private static final SpannableStringBuilder SPANNABLE_STRING_BUILDER = new SpannableStringBuilder();
    private static final StringBuilder STRING_BUILDER = new StringBuilder();
    static final PorterDuffColorFilter ON = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
    static final PorterDuffColorFilter OFF = new PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);

    private final VersePagerPresenter versePagerPresenter;
    private final Resources resources;

    @BindView(R.id.index)
    TextView index;

    @BindView(R.id.text)
    TextView text;

    @BindView(R.id.note_holder)
    TextInputLayout noteHolder;

    @BindView(R.id.note)
    TextInputEditText noteEdit;

    @BindView(R.id.bookmark_icon)
    AppCompatImageView bookmarkIcon;

    @BindView(R.id.note_icon)
    AppCompatImageView noteIcon;

    @BindView(R.id.divider)
    View divider;

    private Verse verse;
    private boolean bookmarked;
    private String note;
    private boolean expanded;

    private VerseIndex verseIndex;

    VerseItemViewHolder(LayoutInflater inflater, ViewGroup parent,
                        VersePagerPresenter versePagerPresenter, Resources resources) {
        super(inflater.inflate(R.layout.item_verse, parent, false));

        this.versePagerPresenter = versePagerPresenter;
        this.resources = resources;

        ButterKnife.bind(this, itemView);
        bookmarkIcon.setOnClickListener(this);
        noteIcon.setOnClickListener(this);
    }

    void bind(Verse verse, int totalVerses, boolean bookmarked, String note, boolean selected, boolean expanded) {
        this.verse = verse;
        this.bookmarked = bookmarked;
        this.note = note;
        this.expanded = expanded;

        itemView.setSelected(selected);

        final Settings settings = versePagerPresenter.getSettings();
        if (settings.isSimpleReading()) {
            itemView.setOnLongClickListener(this);

            if (verse.parallel.size() == 0) {
                divider.setVisibility(View.GONE);

                index.setVisibility(View.VISIBLE);
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

                verseIndex = verse.verseIndex;

                text.setTextColor(textColor);
                text.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                text.setText(verse.text.text);
            } else {
                bindVerse(verse);
            }

            bookmarkIcon.setVisibility(View.GONE);
            noteIcon.setVisibility(View.GONE);
            setExpanded(false);
        } else {
            itemView.setOnLongClickListener(null);

            bindVerse(verse);

            bookmarkIcon.setVisibility(View.VISIBLE);
            bindBookmark(bookmarked);

            noteIcon.setVisibility(View.VISIBLE);

            this.noteEdit.setTextColor(settings.getTextColor());
            this.noteEdit.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(settings.getTextSize().smallerTextSize));
            bindNote(note, expanded);
            setExpanded(expanded);
        }
    }

    private void bindVerse(Verse verse) {
        final Settings settings = versePagerPresenter.getSettings();
        text.setTextColor(settings.getTextColor());
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(settings.getTextSize().textSize));

        if (verse.parallel.size() == 0) {
            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(verse.text.bookName).append(' ')
                    .append(verse.verseIndex.chapter() + 1).append(':').append(verse.verseIndex.verse() + 1).append('\n')
                    .append(verse.text.text);
            text.setText(STRING_BUILDER.toString());
        } else {
            STRING_BUILDER.setLength(0);
            buildVerse(STRING_BUILDER, verse.verseIndex, verse.text);
            final int mainTextLength = STRING_BUILDER.length();
            final int size = verse.parallel.size();
            for (int i = 0; i < size; ++i) {
                final Verse.Text text = verse.parallel.get(i);
                buildVerse(STRING_BUILDER, verse.verseIndex, text);
            }

            SPANNABLE_STRING_BUILDER.clear();
            SPANNABLE_STRING_BUILDER.clearSpans();
            SPANNABLE_STRING_BUILDER.append(STRING_BUILDER);
            final int length = SPANNABLE_STRING_BUILDER.length() - 2;
            SPANNABLE_STRING_BUILDER.setSpan(PARALLEL_VERSE_SIZE_SPAN, mainTextLength, length, 0);
            text.setText(SPANNABLE_STRING_BUILDER.subSequence(0, length));
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

    void bindBookmark(boolean isBookmarked) {
        this.bookmarked = isBookmarked;
        bookmarkIcon.setColorFilter(isBookmarked ? ON : OFF);
    }

    void bindNote(String note, boolean expanded) {
        if (!this.noteEdit.isFocused()) {
            // due to synchronization, the model would trigger the update, so we only update if it's
            // not focused (i.e. user is not typing here)
            this.noteEdit.removeTextChangedListener(this);
            this.noteEdit.setText(note);
            this.noteEdit.addTextChangedListener(this);

            noteIcon.setColorFilter(TextUtils.isEmpty(note) ? OFF : ON);
        }

        noteIcon.setImageResource(expanded ? R.drawable.ic_arrow_up : R.drawable.ic_note);
    }

    private void setExpanded(boolean expanded) {
        this.expanded = expanded;
        noteHolder.setVisibility(expanded ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (verseIndex != null) {
            if (v == bookmarkIcon) {
                if (bookmarked) {
                    versePagerPresenter.removeBookmark(verseIndex);
                } else {
                    versePagerPresenter.addBookmark(verseIndex);
                }
            } else if (v == noteIcon) {
                if (expanded) {
                    versePagerPresenter.hideNote(verseIndex);
                } else {
                    versePagerPresenter.showNote(verseIndex);
                }
                setExpanded(!expanded);
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        final Context context = itemView.getContext();
        if (!(context instanceof FragmentActivity)) {
            return false;
        }

        STRING_BUILDER.setLength(0);
        STRING_BUILDER.append(verse.text.bookName).append(' ')
                .append(verse.verseIndex.chapter() + 1).append(':').append(verse.verseIndex.verse() + 1);

        final FragmentManager fm = ((FragmentActivity) context).getSupportFragmentManager();
        final Fragment fragment = fm.findFragmentByTag(VERSE_DETAIL_FRAGMENT_TAG);
        if (fragment != null) {
            fm.beginTransaction().remove(fragment).commitNowAllowingStateLoss();
        }
        final VerseDetailDialogFragment dialogFragment
                = VerseDetailDialogFragment.newInstance(STRING_BUILDER.toString(), bookmarked, note);
        dialogFragment.setListener(this);
        dialogFragment.show(fm, VERSE_DETAIL_FRAGMENT_TAG);

        return true;
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

    @Override
    public void onVerseDetailUpdated(boolean bookmarked, String note) {
        if (verseIndex != null) {
            if (bookmarked) {
                versePagerPresenter.addBookmark(verseIndex);
            } else {
                versePagerPresenter.removeBookmark(verseIndex);
            }
            if (TextUtils.isEmpty(note)) {
                versePagerPresenter.removeNote(verseIndex);
            } else {
                versePagerPresenter.updateNote(verseIndex, note);
            }
        }
    }
}
