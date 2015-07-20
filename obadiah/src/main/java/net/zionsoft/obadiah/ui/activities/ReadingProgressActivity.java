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

package net.zionsoft.obadiah.ui.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.ReadingProgress;
import net.zionsoft.obadiah.model.ReadingProgressManager;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.ui.adapters.ReadingProgressListAdapter;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ReadingProgressActivity extends BaseAppCompatActivity {
    public static Intent newStartIntent(Context context) {
        return new Intent(context, ReadingProgressActivity.class);
    }

    static class HeaderViewHolder {
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

        HeaderViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    @Inject
    Bible bible;

    @Inject
    ReadingProgressManager readingProgressManager;

    @Inject
    Settings settings;

    @Bind(R.id.loading_spinner)
    View loadingSpinner;

    @Bind(R.id.reading_progress_list_view)
    ListView readingProgressListView;

    private List<String> bookNames;
    private ReadingProgress readingProgress;

    private ReadingProgressListAdapter readingProgressAdapter;

    private HeaderViewHolder headerViewHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.get(this).getInjectionComponent().inject(this);

        initializeUi();

        loadBookNames();
        loadReadingProgress();
    }

    private void initializeUi() {
        setContentView(R.layout.activity_reading_progress);

        final View rootView = getWindow().getDecorView();
        rootView.setKeepScreenOn(settings.keepScreenOn());
        rootView.setBackgroundColor(settings.getBackgroundColor());

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_action_bar);

        loadingSpinner.setVisibility(View.VISIBLE);
        readingProgressListView.setVisibility(View.GONE);

        // list view header
        final int textColor = settings.getTextColor();

        final Resources resources = getResources();
        final float textSize = resources.getDimension(settings.getTextSize().textSize);
        final float smallerTextSize = resources.getDimension(settings.getTextSize().smallerTextSize);

        final View header = LayoutInflater.from(this).inflate(R.layout.item_reading_progress_header,
                readingProgressListView, false);
        headerViewHolder = new HeaderViewHolder(header);

        headerViewHolder.continuousReading.setTextColor(textColor);
        headerViewHolder.continuousReading.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        headerViewHolder.continuousReadingCount.setTextColor(textColor);
        headerViewHolder.continuousReadingCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        headerViewHolder.chapterRead.setTextColor(textColor);
        headerViewHolder.chapterRead.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        headerViewHolder.chapterReadCount.setTextColor(textColor);
        headerViewHolder.chapterReadCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        headerViewHolder.finishedBooks.setTextColor(textColor);
        headerViewHolder.finishedBooks.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        headerViewHolder.finishedBooksCount.setTextColor(textColor);
        headerViewHolder.finishedBooksCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        headerViewHolder.finishedOldTestament.setTextColor(textColor);
        headerViewHolder.finishedOldTestament.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        headerViewHolder.finishedOldTestamentCount.setTextColor(textColor);
        headerViewHolder.finishedOldTestamentCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        headerViewHolder.finishedNewTestament.setTextColor(textColor);
        headerViewHolder.finishedNewTestament.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        headerViewHolder.finishedNewTestamentCount.setTextColor(textColor);
        headerViewHolder.finishedNewTestamentCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        readingProgressListView.addHeaderView(header);

        // list adapter
        readingProgressAdapter = new ReadingProgressListAdapter(this);
        readingProgressListView.setAdapter(readingProgressAdapter);
    }

    private void loadBookNames() {
        bible.loadBookNames(getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
                        .getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null),
                new Bible.OnStringsLoadedListener() {
                    @Override
                    public void onStringsLoaded(List<String> strings) {
                        if (strings == null || strings.size() == 0) {
                            DialogHelper.showDialog(ReadingProgressActivity.this, false, R.string.dialog_retry,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            loadBookNames();
                                        }
                                    }, null
                            );
                            return;
                        }

                        bookNames = strings;
                        updateAdapter();
                    }
                }
        );
    }

    private void loadReadingProgress() {
        readingProgressManager.loadReadingProgress(new ReadingProgressManager.OnReadingProgressLoadedListener() {
            @Override
            public void onReadingProgressLoaded(ReadingProgress readingProgress) {
                if (readingProgress == null) {
                    DialogHelper.showDialog(ReadingProgressActivity.this, false, R.string.dialog_retry,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    loadReadingProgress();
                                }
                            }, null
                    );
                    return;
                }

                headerViewHolder.continuousReadingCount.setText(getString(R.string.text_continuous_reading_count,
                        readingProgress.getContinuousReadingDays()));

                headerViewHolder.chapterReadCount.setText(getString(R.string.text_chapters_read_count,
                        readingProgress.getTotalChapterRead()));

                headerViewHolder.finishedBooksCount.setText(getString(R.string.text_finished_books_count,
                        readingProgress.getFinishedBooksCount()));
                headerViewHolder.finishedOldTestamentCount.setText(getString(R.string.text_finished_old_testament_count,
                        readingProgress.getFinishedOldTestamentCount()));
                headerViewHolder.finishedNewTestamentCount.setText(getString(R.string.text_finished_new_testament_count,
                        readingProgress.getFinishedNewTestamentCount()));

                ReadingProgressActivity.this.readingProgress = readingProgress;
                updateAdapter();
            }
        });
    }

    private void updateAdapter() {
        if (bookNames != null && readingProgress != null) {
            AnimationHelper.fadeOut(loadingSpinner);
            AnimationHelper.fadeIn(readingProgressListView);

            readingProgressAdapter.setData(bookNames, readingProgress);
            readingProgressAdapter.notifyDataSetChanged();
        }
    }
}
