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

import butterknife.InjectView;

public class ReadingProgressActivity extends BaseActionBarActivity {
    public static Intent newStartIntent(Context context) {
        return new Intent(context, ReadingProgressActivity.class);
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

    private TextView mContinuousReadingCountTextView;
    private TextView mChapterReadCountTextView;
    private TextView mFinishedBooksCountTextView;
    private TextView mFinishedOldTestamentCountTextView;
    private TextView mFinishedNewTestamentCountTextView;

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

        final TextView continuousReadingTextView = (TextView) header.findViewById(R.id.continuous_reading_text_view);
        continuousReadingTextView.setTextColor(textColor);
        continuousReadingTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mContinuousReadingCountTextView = (TextView) header.findViewById(R.id.continuous_reading_count_text_view);
        mContinuousReadingCountTextView.setTextColor(textColor);
        mContinuousReadingCountTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        final TextView chapterReadTextView = (TextView) header.findViewById(R.id.chapter_read_text_view);
        chapterReadTextView.setTextColor(textColor);
        chapterReadTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mChapterReadCountTextView = (TextView) header.findViewById(R.id.chapter_read_count_text_view);
        mChapterReadCountTextView.setTextColor(textColor);
        mChapterReadCountTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        final TextView finishedBooksTextView = (TextView) header.findViewById(R.id.finished_books_text_view);
        finishedBooksTextView.setTextColor(textColor);
        finishedBooksTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mFinishedBooksCountTextView = (TextView) header.findViewById(R.id.finished_books_count_text_view);
        mFinishedBooksCountTextView.setTextColor(textColor);
        mFinishedBooksCountTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        final TextView finishedOldTestamentTextView = (TextView) header.findViewById(R.id.finished_old_testament_text_view);
        finishedOldTestamentTextView.setTextColor(textColor);
        finishedOldTestamentTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        mFinishedOldTestamentCountTextView = (TextView) header.findViewById(R.id.finished_old_testament_count_text_view);
        mFinishedOldTestamentCountTextView.setTextColor(textColor);
        mFinishedOldTestamentCountTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        final TextView finishedNewTestamentTextView = (TextView) header.findViewById(R.id.finished_new_testament_text_view);
        finishedNewTestamentTextView.setTextColor(textColor);
        finishedNewTestamentTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        mFinishedNewTestamentCountTextView = (TextView) header.findViewById(R.id.finished_new_testament_count_text_view);
        mFinishedNewTestamentCountTextView.setTextColor(textColor);
        mFinishedNewTestamentCountTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

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

                mContinuousReadingCountTextView.setText(getString(R.string.text_continuous_reading_count,
                        readingProgress.getContinuousReadingDays()));

                mChapterReadCountTextView.setText(getString(R.string.text_chapters_read_count,
                        readingProgress.getTotalChapterRead()));

                mFinishedBooksCountTextView.setText(getString(R.string.text_finished_books_count,
                        readingProgress.getFinishedBooksCount()));
                mFinishedOldTestamentCountTextView.setText(getString(R.string.text_finished_old_testament_count,
                        readingProgress.getFinishedOldTestamentCount()));
                mFinishedNewTestamentCountTextView.setText(getString(R.string.text_finished_new_testament_count,
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
