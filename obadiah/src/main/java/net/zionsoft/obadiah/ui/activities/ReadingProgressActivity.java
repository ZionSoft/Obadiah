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
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.ui.adapters.ReadingProgressListAdapter;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ReadingProgressActivity extends BaseActionBarActivity {
    public static Intent newStartIntent(Context context) {
        return new Intent(context, ReadingProgressActivity.class);
    }

    static class HeaderViewHolder {
        @InjectView(R.id.continuous_reading_text_view)
        TextView continuousReading;

        @InjectView(R.id.continuous_reading_count_text_view)
        TextView continuousReadingCount;

        @InjectView(R.id.chapter_read_text_view)
        TextView chapterRead;

        @InjectView(R.id.chapter_read_count_text_view)
        TextView chapterReadCount;

        @InjectView(R.id.finished_books_text_view)
        TextView finishedBooks;

        @InjectView(R.id.finished_books_count_text_view)
        TextView finishedBooksCount;

        @InjectView(R.id.finished_old_testament_text_view)
        TextView finishedOldTestament;

        @InjectView(R.id.finished_old_testament_count_text_view)
        TextView finishedOldTestamentCount;

        @InjectView(R.id.finished_new_testament_text_view)
        TextView finishedNewTestament;

        @InjectView(R.id.finished_new_testament_count_text_view)
        TextView finishedNewTestamentCount;

        HeaderViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }

    @Inject
    Bible mBible;

    @Inject
    ReadingProgressManager mReadingProgressManager;

    @Inject
    Settings mSettings;

    @InjectView(R.id.loading_spinner)
    View mLoadingSpinner;

    @InjectView(R.id.reading_progress_list_view)
    ListView mReadingProgressListView;

    private List<String> mBookNames;
    private ReadingProgress mReadingProgress;

    private ReadingProgressListAdapter mReadingProgressAdapter;

    private HeaderViewHolder mHeaderViewHolder;

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
        rootView.setKeepScreenOn(mSettings.keepScreenOn());
        rootView.setBackgroundColor(mSettings.getBackgroundColor());

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_action_bar);

        mLoadingSpinner.setVisibility(View.VISIBLE);
        mReadingProgressListView.setVisibility(View.GONE);

        // list view header
        final int textColor = mSettings.getTextColor();

        final Resources resources = getResources();
        final float textSize = resources.getDimension(mSettings.getTextSize().textSize);
        final float smallerTextSize = resources.getDimension(mSettings.getTextSize().smallerTextSize);

        final View header = LayoutInflater.from(this).inflate(R.layout.item_reading_progress_header,
                mReadingProgressListView, false);
        mHeaderViewHolder = new HeaderViewHolder(header);

        mHeaderViewHolder.continuousReading.setTextColor(textColor);
        mHeaderViewHolder.continuousReading.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mHeaderViewHolder.continuousReadingCount.setTextColor(textColor);
        mHeaderViewHolder.continuousReadingCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mHeaderViewHolder.chapterRead.setTextColor(textColor);
        mHeaderViewHolder.chapterRead.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mHeaderViewHolder.chapterReadCount.setTextColor(textColor);
        mHeaderViewHolder.chapterReadCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mHeaderViewHolder.finishedBooks.setTextColor(textColor);
        mHeaderViewHolder.finishedBooks.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mHeaderViewHolder.finishedBooksCount.setTextColor(textColor);
        mHeaderViewHolder.finishedBooksCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mHeaderViewHolder.finishedOldTestament.setTextColor(textColor);
        mHeaderViewHolder.finishedOldTestament.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        mHeaderViewHolder.finishedOldTestamentCount.setTextColor(textColor);
        mHeaderViewHolder.finishedOldTestamentCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        mHeaderViewHolder.finishedNewTestament.setTextColor(textColor);
        mHeaderViewHolder.finishedNewTestament.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        mHeaderViewHolder.finishedNewTestamentCount.setTextColor(textColor);
        mHeaderViewHolder.finishedNewTestamentCount.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        mReadingProgressListView.addHeaderView(header);

        // list adapter
        mReadingProgressAdapter = new ReadingProgressListAdapter(this);
        mReadingProgressListView.setAdapter(mReadingProgressAdapter);
    }

    private void loadBookNames() {
        mBible.loadBookNames(getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
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

                        mBookNames = strings;
                        updateAdapter();
                    }
                }
        );
    }

    private void loadReadingProgress() {
        mReadingProgressManager.loadReadingProgress(new ReadingProgressManager.OnReadingProgressLoadedListener() {
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

                mHeaderViewHolder.continuousReadingCount.setText(getString(R.string.text_continuous_reading_count,
                        readingProgress.getContinuousReadingDays()));

                mHeaderViewHolder.chapterReadCount.setText(getString(R.string.text_chapters_read_count,
                        readingProgress.getTotalChapterRead()));

                mHeaderViewHolder.finishedBooksCount.setText(getString(R.string.text_finished_books_count,
                        readingProgress.getFinishedBooksCount()));
                mHeaderViewHolder.finishedOldTestamentCount.setText(getString(R.string.text_finished_old_testament_count,
                        readingProgress.getFinishedOldTestamentCount()));
                mHeaderViewHolder.finishedNewTestamentCount.setText(getString(R.string.text_finished_new_testament_count,
                        readingProgress.getFinishedNewTestamentCount()));

                mReadingProgress = readingProgress;
                updateAdapter();
            }
        });
    }

    private void updateAdapter() {
        if (mBookNames != null && mReadingProgress != null) {
            AnimationHelper.fadeOut(mLoadingSpinner);
            AnimationHelper.fadeIn(mReadingProgressListView);

            mReadingProgressAdapter.setData(mBookNames, mReadingProgress);
            mReadingProgressAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Analytics.trackScreen(ReadingProgressActivity.class.getSimpleName());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left_to_right);
    }
}
