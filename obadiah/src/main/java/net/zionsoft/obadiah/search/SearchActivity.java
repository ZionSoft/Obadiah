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

package net.zionsoft.obadiah.search;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.android.gms.actions.SearchIntents;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.biblereading.BibleReadingActivity;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.BaseAppCompatActivity;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;

public class SearchActivity extends BaseAppCompatActivity
        implements SearchView.OnQueryTextListener, Toolbar.OnMenuItemClickListener,
        net.zionsoft.obadiah.search.SearchView {
    public static Intent newStartReorderToTopIntent(Context context) {
        final Intent startIntent = new Intent(context, SearchActivity.class);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // there's some horrible issue with FLAG_ACTIVITY_REORDER_TO_FRONT for KitKat and above
            // ref. https://code.google.com/p/android/issues/detail?id=63570
            startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }
        return startIntent;
    }

    private static final String KEY_CURRENT_TRANSLATION = "net.zionsoft.obadiah.search.SearchActivity.KEY_CURRENT_TRANSLATION";
    private static final String KEY_QUERY = "net.zionsoft.obadiah.search.SearchActivity.KEY_QUERY";
    private static final String KEY_VERSES = "net.zionsoft.obadiah.search.SearchActivity.KEY_VERSES";

    @Inject
    SearchPresenter searchPresenter;

    @Inject
    Settings settings;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.search_result_list)
    RecyclerView searchResultList;

    @Bind(R.id.loading_spinner)
    View loadingSpinner;

    private String currentTranslation;
    private String query;
    private ArrayList<Verse> verses;

    private SearchResultListAdapter searchResultAdapter;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(SearchComponentFragment.FRAGMENT_TAG) == null) {
            fm.beginTransaction()
                    .add(SearchComponentFragment.newInstance(),
                            SearchComponentFragment.FRAGMENT_TAG)
                    .commit();
        }

        if (savedInstanceState != null) {
            currentTranslation = savedInstanceState.getString(KEY_CURRENT_TRANSLATION);
            query = savedInstanceState.getString(KEY_QUERY);
            verses = savedInstanceState.getParcelableArrayList(KEY_VERSES);
        }

        setContentView(R.layout.activity_search);
        initializeToolbar();
        searchResultList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        initializeAdapter();

        handleStartIntent(getIntent());
    }

    private void initializeToolbar() {
        toolbar.setLogo(R.drawable.ic_action_bar);

        toolbar.setOnMenuItemClickListener(this);
        toolbar.inflateMenu(R.menu.menu_search);
        final MenuItem searchMenuItem = toolbar.getMenu().findItem(R.id.action_search);
        MenuItemCompat.expandActionView(searchMenuItem);
        searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchView.setSearchableInfo(((SearchManager) getSystemService(SEARCH_SERVICE))
                .getSearchableInfo(getComponentName()));
        searchView.setQueryRefinementEnabled(true);
        searchView.setIconified(false);
        searchView.setOnQueryTextListener(this);

        if (TextUtils.isEmpty(query)) {
            handleStartIntent(getIntent());
        } else {
            searchView.setQuery(query, false);
        }
    }

    private void initializeAdapter() {
        if (searchResultList == null || settings == null || searchPresenter == null || searchResultAdapter != null) {
            // if the activity is recreated due to screen orientation change, the component fragment
            // is called before the UI is initialized, i.e. onAttachFragment() is called inside
            // super.onCreate()
            // therefore, we try to do the initialization in both places
            return;
        }
        searchResultAdapter = new SearchResultListAdapter(this, searchPresenter, settings);
        searchResultAdapter.setVerses(verses);
        searchResultList.setAdapter(searchResultAdapter);
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
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        if (fragment instanceof SearchComponentFragment) {
            ((SearchComponentFragment) fragment).getComponent().inject(this);
            initializeAdapter();
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
        final View rootView = getWindow().getDecorView();
        rootView.setKeepScreenOn(settings.keepScreenOn());
        rootView.setBackgroundColor(settings.getBackgroundColor());

        final String selected = searchPresenter.loadCurrentTranslation();
        if (TextUtils.isEmpty(selected)) {
            throw new IllegalStateException("No translation selected.");
        }
        setTitle(selected);
        if (!selected.equals(currentTranslation)) {
            currentTranslation = selected;
            searchResultAdapter.setVerses(null);
        }
        searchResultAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        searchPresenter.takeView(this);
    }

    @Override
    protected void onPause() {
        searchPresenter.dropView();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_TRANSLATION, currentTranslation);
        outState.putString(KEY_QUERY, query);
        outState.putParcelableArrayList(KEY_VERSES, verses);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        this.query = query;
        search();
        return true;
    }

    private void search() {
        if (TextUtils.isEmpty(query)) {
            return;
        }

        final View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        loadingSpinner.setVisibility(View.VISIBLE);
        searchResultList.setVisibility(View.GONE);

        searchPresenter.search(currentTranslation, query);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_search_history:
                searchPresenter.clearSearchHistory();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onVersesSearched(List<Verse> verses) {
        AnimationHelper.fadeOut(loadingSpinner);
        AnimationHelper.fadeIn(searchResultList);

        searchResultAdapter.setVerses(verses);
        searchResultAdapter.notifyDataSetChanged();
        searchResultList.scrollToPosition(0);

        this.verses = new ArrayList<>(verses);

        String text = getResources().getString(R.string.toast_verses_searched, verses.size());
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVersesSearchFailed() {
        DialogHelper.showDialog(this, false, R.string.dialog_retry,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        search();
                    }
                }, null);
    }

    @Override
    public void openBibleReadingActivity() {
        AnimationHelper.slideIn(this, BibleReadingActivity.newStartReorderToTopIntent(this));
    }
}
