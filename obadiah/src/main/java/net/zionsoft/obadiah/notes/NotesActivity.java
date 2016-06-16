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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Pair;
import android.view.View;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Note;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.BaseRecyclerViewActivity;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

import javax.inject.Inject;

public class NotesActivity extends BaseRecyclerViewActivity implements NotesView {
    @NonNull
    public static Intent newStartIntent(Context context) {
        return new Intent(context, NotesActivity.class);
    }

    @Inject
    NotesPresenter notesPresenter;

    private NotesListAdapter notesListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(NotesComponentFragment.FRAGMENT_TAG) == null) {
            fm.beginTransaction()
                    .add(NotesComponentFragment.newInstance(),
                            NotesComponentFragment.FRAGMENT_TAG)
                    .commit();
        }

        toolbar.setTitle(R.string.activity_notes);
        initializeAdapter();
    }

    private void initializeAdapter() {
        if (recyclerView == null || notesPresenter == null || notesListAdapter != null) {
            // if the activity is recreated due to screen orientation change, the component fragment
            // is attached before the UI is initialized, i.e. onAttachFragment() is called inside
            // super.onCreate()
            // therefore, we try to do the initialization in both places
            return;
        }
        notesListAdapter = new NotesListAdapter(this, notesPresenter.getSettings());
        recyclerView.setAdapter(notesListAdapter);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        if (fragment instanceof NotesComponentFragment) {
            ((NotesComponentFragment) fragment).getComponent().inject(this);

            final View rootView = getWindow().getDecorView();
            final Settings settings = notesPresenter.getSettings();
            rootView.setKeepScreenOn(settings.keepScreenOn());
            rootView.setBackgroundColor(settings.getBackgroundColor());

            initializeAdapter();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        notesPresenter.takeView(this);
        loadNotes();
    }

    private void loadNotes() {
        notesPresenter.loadNotes();
    }

    @Override
    protected void onStop() {
        notesPresenter.dropView();
        super.onStop();
    }

    @Override
    public void onNotesLoaded(List<Note> notes, List<Verse> verses) {
        AnimationHelper.fadeOut(loadingSpinner);
        AnimationHelper.fadeIn(recyclerView);

        notesListAdapter.setNotes(notes, verses);
    }

    @Override
    public void onNotesLoadFailed() {
        DialogHelper.showDialog(this, false, R.string.error_failed_to_load,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadNotes();
                    }
                }, null);
    }

    @Override
    protected void onChildClicked(int position) {
        final Pair<Note, Verse> item = notesListAdapter.getItem(position);
        if (item != null) {
            notesPresenter.saveReadingProgress(item.second.verseIndex);
            Analytics.trackEvent(Analytics.CATEGORY_NOTES, Analytics.NOTES_ACTION_OPENED);
            finish();
        }
    }
}
