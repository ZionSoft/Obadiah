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

package net.zionsoft.obadiah.search;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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
import net.zionsoft.obadiah.model.domain.VerseSearchResult;
import net.zionsoft.obadiah.translations.TranslationManagementActivity;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.BaseAppCompatActivity;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;

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

    private static final String KEY_QUERY = "net.zionsoft.obadiah.KEY_QUERY";

    @Inject
    SearchPresenter searchPresenter;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.search_result_list)
    RecyclerView searchResultList;

    @BindView(R.id.loading_spinner)
    View loadingSpinner;

    private String currentTranslation;
    private String query;

    private SearchResultListAdapter searchResultAdapter;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getSupportFragmentManager();
        SearchComponentFragment componentFragment = (SearchComponentFragment)
                fm.findFragmentByTag(SearchComponentFragment.FRAGMENT_TAG);
        if (componentFragment == null) {
            componentFragment = SearchComponentFragment.newInstance();
            fm.beginTransaction()
                    .add(componentFragment, SearchComponentFragment.FRAGMENT_TAG)
                    .commitNow();
        }
        componentFragment.getComponent().inject(this);

        if (savedInstanceState != null) {
            query = savedInstanceState.getString(KEY_QUERY);
        }
        currentTranslation = searchPresenter.loadCurrentTranslation();

        setContentView(R.layout.activity_search);
        initializeToolbar();
        searchResultList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        searchResultAdapter = new SearchResultListAdapter(this, searchPresenter);
        searchResultList.setAdapter(searchResultAdapter);

        search();
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
        final View rootView = getWindow().getDecorView();
        final Settings settings = searchPresenter.getSettings();
        rootView.setKeepScreenOn(settings.keepScreenOn());
        rootView.setBackgroundColor(settings.getBackgroundColor());

        final String selected = searchPresenter.loadCurrentTranslation();
        if (TextUtils.isEmpty(selected)) {
            // I don't know how, but some users end up here
            // https://fabric.io/zionsoft/android/apps/net.zionsoft.obadiah/issues/578cc3bbffcdc04250ebc8b0
            DialogHelper.showDialog(this, false, R.string.error_no_translation,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(TranslationManagementActivity.newStartIntent(SearchActivity.this));
                        }
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    }
            );
            return;
        }
        setTitle(selected);
        if (!selected.equals(currentTranslation)) {
            currentTranslation = selected;
            searchResultAdapter.setVerses(null);
        }
        searchResultAdapter.notifyDataSetChanged();

        searchPresenter.takeView(this);
    }

    @Override
    protected void onStop() {
        searchPresenter.dropView();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_QUERY, query);
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
    public void onVersesSearched(List<VerseSearchResult> verses) {
        AnimationHelper.fadeOut(loadingSpinner);
        AnimationHelper.fadeIn(searchResultList);

        searchResultAdapter.setVerses(verses);
        searchResultAdapter.notifyDataSetChanged();
        searchResultList.scrollToPosition(0);

        Toast.makeText(this, getString(R.string.toast_verses_searched, verses.size()),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVersesSearchFailed() {
        DialogHelper.showDialog(this, false, R.string.error_failed_to_load,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        search();
                    }
                }, null);
    }

    @Override
    public void openBibleReadingActivity() {
        startActivity(BibleReadingActivity.newStartReorderToTopIntent(this));
    }
}
