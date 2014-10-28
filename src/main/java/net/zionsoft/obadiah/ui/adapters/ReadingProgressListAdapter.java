/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2014 ZionSoft
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

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.ReadingProgress;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.ui.utils.DateFormatter;
import net.zionsoft.obadiah.ui.widget.ProgressBar;

import java.util.List;

public class ReadingProgressListAdapter extends BaseAdapter {
    private static class ViewTag {
        TextView bookName;
        ProgressBar readingProgress;
        TextView lastReadChapter;
    }

    private final int mTextColor;
    private final float mSmallerTextSize;

    private final DateFormatter mDateFormatter;
    private final Resources mResources;
    private final LayoutInflater mInflater;

    private List<String> mBookNames;
    private ReadingProgress mReadingProgress;

    public ReadingProgressListAdapter(Context context) {
        super();

        final Settings settings = Settings.getInstance();
        mTextColor = settings.getTextColor();
        mSmallerTextSize = context.getResources().getDimension(settings.getTextSize().smallerTextSize);

        mResources = context.getResources();
        mDateFormatter = new DateFormatter(mResources);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mBookNames == null || mReadingProgress == null ? 0 : mBookNames.size();
    }

    @Override
    public String getItem(int position) {
        return mBookNames == null || mReadingProgress == null ? null : mBookNames.get(position);
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
            rootView = mInflater.inflate(R.layout.item_reading_progress, parent, false);

            viewTag = new ViewTag();
            viewTag.bookName = (TextView) rootView.findViewById(R.id.book_name_text_view);
            viewTag.bookName.setTextColor(mTextColor);
            viewTag.bookName.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSmallerTextSize);
            viewTag.readingProgress = (ProgressBar) rootView.findViewById(R.id.reading_progress_bar);
            viewTag.lastReadChapter = (TextView) rootView.findViewById(R.id.last_read_chapter_text_view);
            viewTag.lastReadChapter.setTextColor(mTextColor);
            viewTag.lastReadChapter.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSmallerTextSize);
            rootView.setTag(viewTag);
        } else {
            rootView = convertView;
            viewTag = (ViewTag) rootView.getTag();
        }

        viewTag.bookName.setText(mBookNames.get(position));

        final int chaptersRead = mReadingProgress.getChapterRead(position);
        final int chaptersCount = mReadingProgress.getChapterCount(position);
        int progress = chaptersRead * viewTag.readingProgress.getMaxProgress() / chaptersCount;
        if (progress == 0 && chaptersRead > 0) {
            // always show something if progress has been made
            progress = 1;
        }
        viewTag.readingProgress.setProgress(progress);
        viewTag.readingProgress.setText(String.format("%d / %d", chaptersRead, chaptersCount));

        if (chaptersRead > 0) {
            final Pair<Integer, Long> lastReadChapter = mReadingProgress.getLastReadChapter(position);
            viewTag.lastReadChapter.setText(mResources.getString(R.string.text_last_read_chapter,
                    lastReadChapter.first + 1, mDateFormatter.format(lastReadChapter.second)));
            viewTag.lastReadChapter.setVisibility(View.VISIBLE);
        } else {
            viewTag.lastReadChapter.setVisibility(View.GONE);
        }

        return rootView;
    }

    public void setData(List<String> bookNames, ReadingProgress readingProgress) {
        mBookNames = bookNames;
        mReadingProgress = readingProgress;
    }
}
