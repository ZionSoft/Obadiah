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
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.ReadingProgress;
import net.zionsoft.obadiah.ui.utils.DateFormatter;
import net.zionsoft.obadiah.ui.widget.ProgressBar;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

class ReadingProgressListAdapter extends RecyclerView.Adapter {
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final Resources resources;

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
            this.resources = resources;

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

        private void bind(ReadingProgress readingProgress) {
            continuousReadingCount.setText(resources.getString(
                    R.string.text_continuous_reading_count, readingProgress.getContinuousReadingDays()));

            chapterReadCount.setText(resources.getString(
                    R.string.text_chapters_read_count, readingProgress.getTotalChapterRead()));

            finishedBooksCount.setText(resources.getString(
                    R.string.text_finished_books_count, readingProgress.getFinishedBooksCount()));
            finishedOldTestamentCount.setText(resources.getString(
                    R.string.text_finished_old_testament_count, readingProgress.getFinishedOldTestamentCount()));
            finishedNewTestamentCount.setText(resources.getString(
                    R.string.text_finished_new_testament_count, readingProgress.getFinishedNewTestamentCount()));
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.book_name_text_view)
        TextView bookName;

        @Bind(R.id.reading_progress_bar)
        ProgressBar readingProgress;

        @Bind(R.id.last_read_chapter_text_view)
        TextView lastReadChapter;

        private final Resources resources;
        private final DateFormatter dateFormatter;
        private final List<String> bookNames;

        ItemViewHolder(View itemView, Settings settings, Resources resources, List<String> bookNames) {
            super(itemView);

            this.resources = resources;
            this.dateFormatter = new DateFormatter(resources);
            this.bookNames = bookNames;
            ButterKnife.bind(this, itemView);

            final int textColor = settings.getTextColor();
            final float smallerTextSize = resources.getDimension(settings.getTextSize().smallerTextSize);

            bookName.setTextColor(textColor);
            bookName.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
            lastReadChapter.setTextColor(textColor);
            lastReadChapter.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
        }

        private void bind(ReadingProgress readingProgress, int book) {
            bookName.setText(bookNames.get(book));
            final int chaptersRead = readingProgress.getChapterRead(book);
            final int chaptersCount = readingProgress.getChapterCount(book);
            int progress = chaptersRead * this.readingProgress.getMaxProgress() / chaptersCount;
            if (progress == 0 && chaptersRead > 0) {
                // always show something if progress has been made
                progress = 1;
            }
            this.readingProgress.setProgress(progress);
            this.readingProgress.setText(String.format("%d / %d", chaptersRead, chaptersCount));

            if (chaptersRead > 0) {
                final Pair<Integer, Long> lastReadChapter = readingProgress.getLastReadChapter(book);
                this.lastReadChapter.setText(resources.getString(R.string.text_last_read_chapter,
                        lastReadChapter.first + 1, dateFormatter.format(lastReadChapter.second)));
                this.lastReadChapter.setVisibility(View.VISIBLE);
            } else {
                this.lastReadChapter.setVisibility(View.GONE);
            }
        }
    }

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final LayoutInflater inflater;
    private final Resources resources;
    private final Settings settings;

    private final List<String> bookNames;
    private final ReadingProgress readingProgress;

    ReadingProgressListAdapter(Context context, Settings settings,
                               List<String> bookNames, ReadingProgress readingProgress) {
        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
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
                        settings, resources, bookNames);
            default:
                throw new IllegalStateException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_HEADER:
                ((HeaderViewHolder) holder).bind(readingProgress);
                break;
            case VIEW_TYPE_ITEM:
                ((ItemViewHolder) holder).bind(readingProgress, position - 1);
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
