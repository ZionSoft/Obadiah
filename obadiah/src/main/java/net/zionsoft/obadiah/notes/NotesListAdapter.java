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

package net.zionsoft.obadiah.notes;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Note;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.ui.utils.DateFormatter;
import net.zionsoft.obadiah.ui.widget.SectionHeader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class NotesListAdapter extends RecyclerView.Adapter {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_NOTE = 1;

    static class ViewHolder extends RecyclerView.ViewHolder {
        private static final StringBuilder STRING_BUILDER = new StringBuilder();

        @Bind(R.id.title)
        TextView title;

        @Bind(R.id.verse)
        TextView verse;

        @Bind(R.id.note)
        TextView note;

        private ViewHolder(View itemView, int textColor, float textSize, float smallerTextSize) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            title.setTextColor(textColor);
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            verse.setTextColor(textColor);
            verse.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
            note.setTextColor(textColor);
            note.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
        }

        private void bind(Note note, Verse verse) {
            this.note.setText(note.note);

            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(verse.text.bookName).append(' ')
                    .append(verse.verseIndex.chapter + 1).append(':').append(verse.verseIndex.verse + 1);
            title.setText(STRING_BUILDER.toString());
            this.verse.setText(verse.text.text);
        }
    }

    private final LayoutInflater inflater;
    private final int textColor;
    private final float textSize;
    private final float smallerTextSize;

    private final DateFormatter dateFormatter;

    private final ArrayList<String> sectionHeaders = new ArrayList<>();
    private ArrayList<ArrayList<Pair<Note, Verse>>> notesByDay = new ArrayList<>();
    private int count = 0;

    NotesListAdapter(Context context, NotesPresenter notesPresenter) {
        this.inflater = LayoutInflater.from(context);

        final Resources resources = context.getResources();
        final Settings settings = notesPresenter.getSettings();
        this.textColor = settings.getTextColor();
        final Settings.TextSize textSize = settings.getTextSize();
        this.textSize = resources.getDimension(textSize.textSize);
        this.smallerTextSize = resources.getDimension(textSize.smallerTextSize);

        this.dateFormatter = new DateFormatter(resources);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        }

        final int size = notesByDay.size();
        for (int i = 0; i < size; ++i) {
            position -= notesByDay.get(i).size() + 1;
            if (position < 0) {
                return VIEW_TYPE_NOTE;
            } else if (position == 0) {
                return VIEW_TYPE_HEADER;
            }
        }

        throw new IllegalStateException("Unknown view type for position - " + position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                final SectionHeader header = (SectionHeader)
                        inflater.inflate(R.layout.item_section_header, parent, false);
                header.setHeaderTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                return new RecyclerView.ViewHolder(header) {
                };
            case VIEW_TYPE_NOTE:
                return new ViewHolder(inflater.inflate(R.layout.item_note, parent, false),
                        textColor, textSize, smallerTextSize);
            default:
                throw new IllegalStateException("Unknown view type - " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position == 0) {
            // VIEW_TYPE_HEADER
            ((SectionHeader) holder.itemView).setHeaderText(sectionHeaders.get(0));
            return;
        }

        final int versesByDaySize = notesByDay.size();
        for (int i = 0; i < versesByDaySize; ++i) {
            final ArrayList<Pair<Note, Verse>> notes = notesByDay.get(i);
            --position;
            final int size = notes.size();
            if (position < size) {
                // VIEW_TYPE_NOTE
                final Pair<Note, Verse> note = notes.get(position);
                ((ViewHolder) holder).bind(note.first, note.second);
                return;
            }

            position -= size;
            if (position == 0) {
                // VIEW_TYPE_HEADER
                ((SectionHeader) holder.itemView).setHeaderText(sectionHeaders.get(i + 1));
                return;
            }
        }

        throw new IllegalStateException("Unknown view type for position - " + position);
    }

    @Override
    public int getItemCount() {
        return count;
    }

    void setNotes(List<Note> notes, List<Verse> verses) {
        sectionHeaders.clear();
        notesByDay.clear();

        final Calendar calendar = Calendar.getInstance();
        int previousYear = -1;
        int previousDayOfYear = -1;
        ArrayList<Pair<Note, Verse>> notesOfSameDay = null;
        final int notesCount = notes.size();
        for (int i = 0; i < notesCount; ++i) {
            final Note note = notes.get(i);
            final long timestamp = note.timestamp;
            calendar.setTimeInMillis(timestamp);
            final int currentYear = calendar.get(Calendar.YEAR);
            final int currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
            if (previousDayOfYear != currentDayOfYear || previousYear != currentYear) {
                ++count;
                sectionHeaders.add(dateFormatter.format(timestamp));

                notesOfSameDay = new ArrayList<>();
                notesByDay.add(notesOfSameDay);

                previousYear = currentYear;
                previousDayOfYear = currentDayOfYear;
            }
            ++count;

            assert notesOfSameDay != null;
            notesOfSameDay.add(new Pair<>(note, verses.get(i)));
        }

        notifyDataSetChanged();
    }

    @Nullable
    Verse getVerse(int position) {
        if (position == 0) {
            // VIEW_TYPE_HEADER
            return null;
        }

        final int versesByDaySize = notesByDay.size();
        for (int i = 0; i < versesByDaySize; ++i) {
            final ArrayList<Pair<Note, Verse>> notes = notesByDay.get(i);
            --position;
            final int size = notes.size();
            if (position < size) {
                // VIEW_TYPE_NOTE
                return notes.get(position).second;
            }

            position -= size;
            if (position == 0) {
                // VIEW_TYPE_HEADER
                return null;
            }
        }

        throw new IllegalStateException("Unknown view type for position - " + position);
    }
}
