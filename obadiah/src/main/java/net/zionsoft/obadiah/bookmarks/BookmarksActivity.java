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

package net.zionsoft.obadiah.bookmarks;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.BaseAppCompatActivity;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;

public class BookmarksActivity extends BaseAppCompatActivity implements BookmarksView,
        RecyclerView.OnChildAttachStateChangeListener, View.OnClickListener {
    @NonNull
    public static Intent newStartIntent(Context context) {
        return new Intent(context, BookmarksActivity.class);
    }

    @Inject
    BookmarksPresenter bookmarksPresenter;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.bookmark_list)
    RecyclerView bookmarkList;

    @Bind(R.id.loading_spinner)
    View loadingSpinner;

    private BookmarksListAdapter bookmarksListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(BookmarksComponentFragment.FRAGMENT_TAG) == null) {
            fm.beginTransaction()
                    .add(BookmarksComponentFragment.newInstance(),
                            BookmarksComponentFragment.FRAGMENT_TAG)
                    .commit();
        }

        setContentView(R.layout.activity_bookmarks);

        toolbar.setLogo(R.drawable.ic_action_bar);
        toolbar.setTitle(R.string.activity_bookmarks);

        bookmarkList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        bookmarkList.addOnChildAttachStateChangeListener(this);
        initializeAdapter();
    }

    private void initializeAdapter() {
        if (bookmarkList == null || bookmarksPresenter == null || bookmarksListAdapter != null) {
            // if the activity is recreated due to screen orientation change, the component fragment
            // is attached before the UI is initialized, i.e. onAttachFragment() is called inside
            // super.onCreate()
            // therefore, we try to do the initialization in both places
            return;
        }
        bookmarksListAdapter = new BookmarksListAdapter(this, bookmarksPresenter);
        bookmarkList.setAdapter(bookmarksListAdapter);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        if (fragment instanceof BookmarksComponentFragment) {
            ((BookmarksComponentFragment) fragment).getComponent().inject(this);

            final View rootView = getWindow().getDecorView();
            final Settings settings = bookmarksPresenter.getSettings();
            rootView.setKeepScreenOn(settings.keepScreenOn());
            rootView.setBackgroundColor(settings.getBackgroundColor());

            initializeAdapter();
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        bookmarksPresenter.takeView(this);
        loadBookmarks();
    }

    private void loadBookmarks() {
        bookmarksPresenter.loadBookmarks();
    }

    @Override
    protected void onPause() {
        bookmarksPresenter.dropView();
        super.onPause();
    }

    @Override
    public void onBookmarksLoaded(List<Bookmark> bookmarks, List<Verse> verses) {
        AnimationHelper.fadeOut(loadingSpinner);
        AnimationHelper.fadeIn(bookmarkList);

        bookmarksListAdapter.setBookmarks(bookmarks, verses);
    }

    @Override
    public void onBookmarksLoadFailed() {
        DialogHelper.showDialog(this, false, R.string.error_failed_to_load,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadBookmarks();
                    }
                }, null);
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
        view.setOnClickListener(this);
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        view.setOnClickListener(null);
    }

    @Override
    public void onClick(View v) {
        final int position = bookmarkList.getChildAdapterPosition(v);
        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        final Verse verse = bookmarksListAdapter.getVerse(position);
        if (verse != null) {
            bookmarksPresenter.saveReadingProgress(verse.verseIndex);
            finish();
        }
    }
}
