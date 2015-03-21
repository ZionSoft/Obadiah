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

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
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

import com.google.android.gms.actions.SearchIntents;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.BookSelectionActivity;
import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.Verse;
import net.zionsoft.obadiah.model.search.RecentSearchProvider;
import net.zionsoft.obadiah.ui.adapters.SearchResultListAdapter;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

import javax.inject.Inject;

import butterknife.InjectView;

public class SearchActivity extends BaseActionBarActivity {
    public static Intent newStartReorderToTopIntent(Context context) {
        final Intent startIntent = new Intent(context, SearchActivity.class);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // there's some horrible issue with FLAG_ACTIVITY_REORDER_TO_FRONT for KitKat and above
            // ref. https://code.google.com/p/android/issues/detail?id=63570
            startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }
        return startIntent;
    }

    private static class NonConfigurationData {
        String currentTranslation;
        List<Verse> verses;
    }

    @Inject
    Bible bible;

    @Inject
    Settings settings;

    @InjectView(R.id.loading_spinner)
    View loadingSpinner;

    @InjectView(R.id.search_result_list_view)
    ListView searchResultListView;

    private NonConfigurationData nonConfigurationData;

    private SearchRecentSuggestions recentSearches;

    private SearchResultListAdapter searchResultListAdapter;

    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.get(this).getInjectionComponent().inject(this);

        recentSearches = new SearchRecentSuggestions(this,
                RecentSearchProvider.AUTHORITY, RecentSearchProvider.MODE);

        initializeUi();

        nonConfigurationData = (NonConfigurationData) getLastCustomNonConfigurationInstance();
        if (nonConfigurationData == null) {
            nonConfigurationData = new NonConfigurationData();
        } else {
            searchResultListAdapter.setVerses(nonConfigurationData.verses);
        }

        handleStartIntent(getIntent());
    }

    private void initializeUi() {
        setContentView(R.layout.activity_search);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_action_bar);

        loadingSpinner.setVisibility(View.GONE);

        searchResultListAdapter = new SearchResultListAdapter(this);
        searchResultListView.setAdapter(searchResultListAdapter);
        searchResultListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Verse verse = nonConfigurationData.verses.get(position);
                getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE).edit()
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
        final String action = intent.getAction();
        if (searchView != null
                && (SearchIntents.ACTION_SEARCH.equals(action) || Intent.ACTION_SEARCH.equals(action))) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(query)) {
                searchView.setQuery(query, true);
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
        settings.refresh();

        final View rootView = getWindow().getDecorView();
        rootView.setKeepScreenOn(settings.keepScreenOn());
        rootView.setBackgroundColor(settings.getBackgroundColor());

        final String selected = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
                .getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
        setTitle(selected);
        if (!selected.equals(nonConfigurationData.currentTranslation)) {
            nonConfigurationData.currentTranslation = selected;

            searchResultListAdapter.setVerses(null);
        }
        searchResultListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);

        final MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        MenuItemCompat.expandActionView(searchMenuItem);
        searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setSearchableInfo(((SearchManager) getSystemService(SEARCH_SERVICE))
                .getSearchableInfo(getComponentName()));
        searchView.setQueryRefinementEnabled(true);
        searchView.setIconified(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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

        handleStartIntent(getIntent());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_search_history:
                recentSearches.clearHistory();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return nonConfigurationData;
    }

    private void search(final String query) {
        if (TextUtils.isEmpty(query)) {
            return;
        }

        final View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        loadingSpinner.setVisibility(View.VISIBLE);
        searchResultListView.setVisibility(View.GONE);

        recentSearches.saveRecentQuery(query, null);
        bible.searchVerses(nonConfigurationData.currentTranslation, query,
                new Bible.OnVersesLoadedListener() {
                    @Override
                    public void onVersesLoaded(List<Verse> verses) {
                        if (verses == null) {
                            DialogHelper.showDialog(SearchActivity.this, false, R.string.dialog_retry,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            search(query);
                                        }
                                    }, null
                            );
                            return;
                        }

                        AnimationHelper.fadeOut(loadingSpinner);
                        AnimationHelper.fadeIn(searchResultListView);

                        searchResultListAdapter.setVerses(verses);
                        searchResultListAdapter.notifyDataSetChanged();
                        searchResultListView.post(new Runnable() {
                            @Override
                            public void run() {
                                searchResultListView.setSelection(0);
                            }
                        });

                        nonConfigurationData.verses = verses;

                        String text = getResources().getString(R.string.toast_verses_searched, verses.size());
                        Toast.makeText(SearchActivity.this, text, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}
