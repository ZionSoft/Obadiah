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

package net.zionsoft.obadiah.ui.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.ReadingProgress;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.ui.utils.DateFormatter;
import net.zionsoft.obadiah.ui.widget.ProgressBar;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ReadingProgressListAdapter extends BaseAdapter {
    static class ViewTag {
        @InjectView(R.id.book_name_text_view)
        TextView bookName;

        @InjectView(R.id.reading_progress_bar)
        ProgressBar readingProgress;

        @InjectView(R.id.last_read_chapter_text_view)
        TextView lastReadChapter;

        ViewTag(View view) {
            ButterKnife.inject(this, view);
        }
    }

    @Inject
    Settings settings;

    private final int textColor;
    private final float smallerTextSize;

    private final DateFormatter dateFormatter;
    private final Resources resources;
    private final LayoutInflater inflater;

    private List<String> bookNames;
    private ReadingProgress readingProgress;

    public ReadingProgressListAdapter(Context context) {
        super();
        App.get(context).getInjectionComponent().inject(this);

        textColor = settings.getTextColor();
        smallerTextSize = context.getResources().getDimension(settings.getTextSize().smallerTextSize);

        resources = context.getResources();
        dateFormatter = new DateFormatter(resources);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return bookNames == null || readingProgress == null ? 0 : bookNames.size();
    }

    @Override
    public String getItem(int position) {
        return bookNames == null || readingProgress == null ? null : bookNames.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View rootView;
        final ViewTag viewTag;
        if (convertView == null) {
            rootView = inflater.inflate(R.layout.item_reading_progress, parent, false);

            viewTag = new ViewTag(rootView);
            viewTag.bookName.setTextColor(textColor);
            viewTag.bookName.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
            viewTag.lastReadChapter.setTextColor(textColor);
            viewTag.lastReadChapter.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
            rootView.setTag(viewTag);
        } else {
            rootView = convertView;
            viewTag = (ViewTag) rootView.getTag();
        }

        viewTag.bookName.setText(bookNames.get(position));

        final int chaptersRead = readingProgress.getChapterRead(position);
        final int chaptersCount = readingProgress.getChapterCount(position);
        int progress = chaptersRead * viewTag.readingProgress.getMaxProgress() / chaptersCount;
        if (progress == 0 && chaptersRead > 0) {
            // always show something if progress has been made
            progress = 1;
        }
        viewTag.readingProgress.setProgress(progress);
        viewTag.readingProgress.setText(String.format("%d / %d", chaptersRead, chaptersCount));

        if (chaptersRead > 0) {
            final Pair<Integer, Long> lastReadChapter = readingProgress.getLastReadChapter(position);
            viewTag.lastReadChapter.setText(resources.getString(R.string.text_last_read_chapter,
                    lastReadChapter.first + 1, dateFormatter.format(lastReadChapter.second)));
            viewTag.lastReadChapter.setVisibility(View.VISIBLE);
        } else {
            viewTag.lastReadChapter.setVisibility(View.GONE);
        }

        return rootView;
    }

    public void setData(List<String> bookNames, ReadingProgress readingProgress) {
        this.bookNames = bookNames;
        this.readingProgress = readingProgress;
    }
}
