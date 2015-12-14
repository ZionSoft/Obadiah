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

package net.zionsoft.obadiah.readingprogress;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.domain.ReadingProgress;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.ui.utils.DateFormatter;
import net.zionsoft.obadiah.ui.widget.ProgressBar;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

class ReadingProgressListAdapter extends RecyclerView.Adapter {
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.continuous_reading_text_view)
        TextView continuousReading;

        @Bind(R.id.continuous_reading_count_text_view)
        TextView continuousReadingCount;

        @Bind(R.id.chapter_read_text_view)
        TextView chapterRead;

        @Bind(R.id.chapter_read_count_text_view)
        TextView chapterReadCount;

        @Bind(R.id.finished_books_text_view)
        TextView finishedBooks;

        @Bind(R.id.finished_books_count_text_view)
        TextView finishedBooksCount;

        @Bind(R.id.finished_old_testament_text_view)
        TextView finishedOldTestament;

        @Bind(R.id.finished_old_testament_count_text_view)
        TextView finishedOldTestamentCount;

        @Bind(R.id.finished_new_testament_text_view)
        TextView finishedNewTestament;

        @Bind(R.id.finished_new_testament_count_text_view)
        TextView finishedNewTestamentCount;

        HeaderViewHolder(View itemView, Settings settings, Resources resources) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            final int textColor = settings.getTextColor();
            final float textSize = resources.getDimension(settings.getTextSize().textSize);
            final float smallerTextSize = resources.getDimension(settings.getTextSize().smallerTextSize);

            continuousReading.setTextColor(textColor);
            continuousReading.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

            continuousReadingCount.setTextColor(textColor);
            continuousReadingCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

            chapterRead.setTextColor(textColor);
            chapterRead.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

            chapterReadCount.setTextColor(textColor);
            chapterReadCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

            finishedBooks.setTextColor(textColor);
            finishedBooks.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

            finishedBooksCount.setTextColor(textColor);
            finishedBooksCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

            finishedOldTestament.setTextColor(textColor);
            finishedOldTestament.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

            finishedOldTestamentCount.setTextColor(textColor);
            finishedOldTestamentCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

            finishedNewTestament.setTextColor(textColor);
            finishedNewTestament.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

            finishedNewTestamentCount.setTextColor(textColor);
            finishedNewTestamentCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.book_name_text_view)
        TextView bookName;

        @Bind(R.id.reading_progress_bar)
        ProgressBar readingProgress;

        @Bind(R.id.last_read_chapter_text_view)
        TextView lastReadChapter;

        ItemViewHolder(View itemView, Settings settings, Resources resources) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            final int textColor = settings.getTextColor();
            final float smallerTextSize = resources.getDimension(settings.getTextSize().smallerTextSize);

            bookName.setTextColor(textColor);
            bookName.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
            lastReadChapter.setTextColor(textColor);
            lastReadChapter.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
        }
    }

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final LayoutInflater inflater;
    private final Resources resources;
    private final DateFormatter dateFormatter;
    private final Settings settings;

    private final List<String> bookNames;
    private final ReadingProgress readingProgress;

    ReadingProgressListAdapter(Context context, Settings settings,
                               List<String> bookNames, ReadingProgress readingProgress) {
        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
        this.dateFormatter = new DateFormatter(resources);
        this.settings = settings;
        this.bookNames = bookNames;
        this.readingProgress = readingProgress;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                return new HeaderViewHolder(inflater.inflate(R.layout.item_reading_progress_header, parent, false),
                        settings, resources);
            case VIEW_TYPE_ITEM:
                return new ItemViewHolder(inflater.inflate(R.layout.item_reading_progress, parent, false),
                        settings, resources);
            default:
                throw new IllegalStateException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_HEADER:
                HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
                headerViewHolder.continuousReadingCount.setText(resources.getString(
                        R.string.text_continuous_reading_count, readingProgress.getContinuousReadingDays()));

                headerViewHolder.chapterReadCount.setText(resources.getString(
                        R.string.text_chapters_read_count, readingProgress.getTotalChapterRead()));

                headerViewHolder.finishedBooksCount.setText(resources.getString(
                        R.string.text_finished_books_count, readingProgress.getFinishedBooksCount()));
                headerViewHolder.finishedOldTestamentCount.setText(resources.getString(
                        R.string.text_finished_old_testament_count, readingProgress.getFinishedOldTestamentCount()));
                headerViewHolder.finishedNewTestamentCount.setText(resources.getString(
                        R.string.text_finished_new_testament_count, readingProgress.getFinishedNewTestamentCount()));
                break;
            case VIEW_TYPE_ITEM:
                --position;
                ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
                itemViewHolder.bookName.setText(bookNames.get(position));
                final int chaptersRead = readingProgress.getChapterRead(position);
                final int chaptersCount = readingProgress.getChapterCount(position);
                int progress = chaptersRead * itemViewHolder.readingProgress.getMaxProgress() / chaptersCount;
                if (progress == 0 && chaptersRead > 0) {
                    // always show something if progress has been made
                    progress = 1;
                }
                itemViewHolder.readingProgress.setProgress(progress);
                itemViewHolder.readingProgress.setText(String.format("%d / %d", chaptersRead, chaptersCount));

                if (chaptersRead > 0) {
                    final Pair<Integer, Long> lastReadChapter = readingProgress.getLastReadChapter(position);
                    itemViewHolder.lastReadChapter.setText(resources.getString(R.string.text_last_read_chapter,
                            lastReadChapter.first + 1, dateFormatter.format(lastReadChapter.second)));
                    itemViewHolder.lastReadChapter.setVisibility(View.VISIBLE);
                } else {
                    itemViewHolder.lastReadChapter.setVisibility(View.GONE);
                }
                break;
            default:
                throw new IllegalStateException("Unknown view type: " + getItemViewType(position));
        }
    }

    @Override
    public int getItemCount() {
        return bookNames.size() + 1;
    }
}
