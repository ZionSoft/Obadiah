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

package net.zionsoft.obadiah.readingprogress;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.ReadingProgress;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.BaseAppCompatActivity;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;

public class ReadingProgressActivity extends BaseAppCompatActivity implements ReadingProgressView {
    public static Intent newStartIntent(Context context) {
        return new Intent(context, ReadingProgressActivity.class);
    }

    @Inject
    ReadingProgressPresenter readingProgressPresenter;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.reading_progress_list)
    RecyclerView readingProgressList;

    @BindView(R.id.loading_spinner)
    View loadingSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getSupportFragmentManager();
        ReadingProgressComponentFragment componentFragment = (ReadingProgressComponentFragment)
                fm.findFragmentByTag(ReadingProgressComponentFragment.FRAGMENT_TAG);
        if (componentFragment == null) {
            componentFragment = ReadingProgressComponentFragment.newInstance();
            fm.beginTransaction()
                    .add(componentFragment, ReadingProgressComponentFragment.FRAGMENT_TAG)
                    .commitNow();
        }
        componentFragment.getComponent().inject(this);

        setContentView(R.layout.activity_reading_progress);

        final View rootView = getWindow().getDecorView();
        final Settings settings = readingProgressPresenter.getSettings();
        rootView.setKeepScreenOn(settings.keepScreenOn());
        rootView.setBackgroundColor(settings.getBackgroundColor());

        toolbar.setLogo(R.drawable.ic_action_bar);
        toolbar.setTitle(R.string.activity_reading_progress);
        readingProgressList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
    }

    @Override
    protected void onStart() {
        super.onStart();
        readingProgressPresenter.takeView(this);
        loadReadingProgress();
    }

    void loadReadingProgress() {
        readingProgressPresenter.loadReadingProgress();
    }

    @Override
    protected void onStop() {
        readingProgressPresenter.dropView();
        super.onStop();
    }

    @Override
    public void onReadingProgressLoaded(ReadingProgress readingProgress, List<String> bookNames) {
        AnimationHelper.fadeOut(loadingSpinner);
        readingProgressList.setAdapter(new ReadingProgressListAdapter(
                this, readingProgressPresenter.getSettings(), bookNames, readingProgress));
    }

    @Override
    public void onReadingProgressLoadFailed() {
        DialogHelper.showDialog(this, false, R.string.error_failed_to_load,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadReadingProgress();
                    }
                }, null);
    }
}
