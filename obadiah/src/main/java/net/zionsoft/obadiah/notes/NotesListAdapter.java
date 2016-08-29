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
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Note;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.ui.utils.BaseSectionAdapter;
import net.zionsoft.obadiah.ui.utils.DateFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

class NotesListAdapter extends BaseSectionAdapter<Pair<Note, Verse>> {
    static class ViewHolder extends RecyclerView.ViewHolder {
        private static final StringBuilder STRING_BUILDER = new StringBuilder();

        @BindView(R.id.title)
        TextView title;

        @BindView(R.id.verse)
        TextView verse;

        @BindView(R.id.note)
        TextView note;

        ViewHolder(View itemView, int textColor, float textSize, float smallerTextSize) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            title.setTextColor(textColor);
            title.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            verse.setTextColor(textColor);
            verse.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
            note.setTextColor(textColor);
            note.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
        }

        void bind(Note note, Verse verse) {
            this.note.setText(note.note());

            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(verse.text.bookName).append(' ')
                    .append(verse.verseIndex.chapter() + 1).append(':').append(verse.verseIndex.verse() + 1);
            title.setText(STRING_BUILDER.toString());
            this.verse.setText(verse.text.text);
        }
    }

    private final DateFormatter dateFormatter;

    NotesListAdapter(Context context, Settings settings) {
        super(context, settings);
        this.dateFormatter = new DateFormatter(context.getResources());
    }

    @Override
    protected RecyclerView.ViewHolder createItemViewHolder(ViewGroup parent) {
        return new ViewHolder(inflater.inflate(R.layout.item_note, parent, false),
                textColor, textSize, smallerTextSize);
    }

    @Override
    protected void bindItemViewHeader(RecyclerView.ViewHolder holder, Pair<Note, Verse> item) {
        ((ViewHolder) holder).bind(item.first, item.second);
    }

    void setNotes(List<Note> notes, List<Verse> verses) {
        final ArrayList<String> headers = new ArrayList<>();
        final ArrayList<ArrayList<Pair<Note, Verse>>> notesByDay = new ArrayList<>();
        int count = 0;

        final Calendar calendar = Calendar.getInstance();
        int previousYear = -1;
        int previousDayOfYear = -1;
        ArrayList<Pair<Note, Verse>> notesOfSameDay = null;
        final int notesCount = notes.size();
        for (int i = 0; i < notesCount; ++i) {
            final Note note = notes.get(i);
            final long timestamp = note.timestamp();
            calendar.setTimeInMillis(timestamp);
            final int currentYear = calendar.get(Calendar.YEAR);
            final int currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
            if (previousDayOfYear != currentDayOfYear || previousYear != currentYear) {
                ++count;
                headers.add(dateFormatter.format(timestamp));

                notesOfSameDay = new ArrayList<>();
                notesByDay.add(notesOfSameDay);

                previousYear = currentYear;
                previousDayOfYear = currentDayOfYear;
            }
            ++count;

            assert notesOfSameDay != null;
            notesOfSameDay.add(new Pair<>(note, verses.get(i)));
        }

        setData(headers, notesByDay, count);
    }
}
