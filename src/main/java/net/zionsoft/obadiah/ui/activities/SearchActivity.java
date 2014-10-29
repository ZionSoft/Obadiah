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

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import net.zionsoft.obadiah.BookSelectionActivity;
import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.Verse;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.search.RecentSearchProvider;
import net.zionsoft.obadiah.ui.adapters.SearchResultListAdapter;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

public class SearchActivity extends ActionBarActivity {
    public static Intent newStartReorderToTopIntent(Context context) {
        return new Intent(context, SearchActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    }

    private static class NonConfigurationData {
        String currentTranslation;
        List<Verse> verses;
    }

    private NonConfigurationData mData;

    private Settings mSettings;
    private SharedPreferences mPreferences;
    private SearchRecentSuggestions mRecentSearches;

    private SearchResultListAdapter mSearchResultListAdapter;

    private View mRootView;
    private View mLoadingSpinner;
    private ListView mSearchResultListView;
    private SearchView mSearchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = Settings.getInstance();
        mPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        mRecentSearches = new SearchRecentSuggestions(this,
                RecentSearchProvider.AUTHORITY, RecentSearchProvider.MODE);

        initializeUi();

        mData = (NonConfigurationData) getLastCustomNonConfigurationInstance();
        if (mData == null) {
            mData = new NonConfigurationData();
        } else {
            mSearchResultListAdapter.setVerses(mData.verses);
        }

        handleStartIntent(getIntent());
    }

    private void initializeUi() {
        setContentView(R.layout.activity_search);

        mRootView = getWindow().getDecorView();

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_action_bar);

        mLoadingSpinner = findViewById(R.id.loading_spinner);
        mLoadingSpinner.setVisibility(View.GONE);

        mSearchResultListView = (ListView) findViewById(R.id.search_result_list_view);
        mSearchResultListAdapter = new SearchResultListAdapter(this);
        mSearchResultListView.setAdapter(mSearchResultListAdapter);
        mSearchResultListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Verse verse = mData.verses.get(position);
                mPreferences.edit()
                        .putInt(Constants.PREF_KEY_LAST_READ_BOOK, verse.bookIndex)
                        .putInt(Constants.PREF_KEY_LAST_READ_CHAPTER, verse.chapterIndex)
                        .putInt(Constants.PREF_KEY_LAST_READ_VERSE, verse.verseIndex)
                        .apply();

                AnimationHelper.slideIn(SearchActivity.this,
                        BookSelectionActivity.newStartReorderToTopIntent(SearchActivity.this));
            }
        });
    }

    private void handleStartIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction()) && mSearchView != null) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(query)) {
                mSearchView.setQuery(query, true);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
        handleStartIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        populateUi();
    }

    private void populateUi() {
        mSettings.refresh();
        mRootView.setKeepScreenOn(mSettings.keepScreenOn());
        mRootView.setBackgroundColor(mSettings.getBackgroundColor());

        final String selected = mPreferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
        setTitle(selected);
        if (!selected.equals(mData.currentTranslation)) {
            mData.currentTranslation = selected;

            mSearchResultListAdapter.setVerses(null);
        }
        mSearchResultListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Analytics.trackScreen(SearchActivity.class.getSimpleName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        final MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        MenuItemCompat.expandActionView(searchMenuItem);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        mSearchView.setSearchableInfo(((SearchManager) getSystemService(SEARCH_SERVICE))
                .getSearchableInfo(getComponentName()));
        mSearchView.setQueryRefinementEnabled(true);
        mSearchView.setIconified(false);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                search(query);
                return true;
            }
        });

        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left_to_right);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return mData;
    }

    private void search(final String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            return;
        }

        final View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        mLoadingSpinner.setVisibility(View.VISIBLE);
        mSearchResultListView.setVisibility(View.GONE);

        mRecentSearches.saveRecentQuery(keyword, null);
        Bible.getInstance().searchVerses(mData.currentTranslation, keyword,
                new Bible.OnVersesLoadedListener() {
                    @Override
                    public void onVersesLoaded(List<Verse> verses) {
                        if (verses == null) {
                            DialogHelper.showDialog(SearchActivity.this, false, R.string.dialog_retry,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            search(keyword);
                                        }
                                    }, null
                            );
                            return;
                        }

                        AnimationHelper.fadeOut(mLoadingSpinner);
                        AnimationHelper.fadeIn(mSearchResultListView);

                        mSearchResultListAdapter.setVerses(verses);
                        mSearchResultListAdapter.notifyDataSetChanged();
                        mSearchResultListView.post(new Runnable() {
                            @Override
                            public void run() {
                                mSearchResultListView.setSelection(0);
                            }
                        });

                        mData.verses = verses;

                        String text = getResources().getString(R.string.toast_verses_searched, verses.size());
                        Toast.makeText(SearchActivity.this, text, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}
