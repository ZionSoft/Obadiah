/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2013 ZionSoft
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

package net.zionsoft.obadiah;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import net.zionsoft.obadiah.bible.TranslationReader;
import net.zionsoft.obadiah.util.SettingsManager;

import java.util.List;

public class SearchActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        mSettingsManager = new SettingsManager(this);
        mTranslationReader = new TranslationReader(this);

        initializeUi();

        mData = (NonConfigurationData) getLastCustomNonConfigurationInstance();
        if (mData == null) {
            mData = new NonConfigurationData();
        } else {
            mSearchText.setText(mData.searchText);
            mSearchResultListAdapter.setSearchResults(mData.results);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSettingsManager.refresh();
        populateUi();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        mData.searchText = mSearchText.getText();
        return mData;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select_translation:
                startActivity(new Intent(SearchActivity.this, TranslationSelectionActivity.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void initializeUi() {
        mRootView = getWindow().getDecorView();

        // initializes the search bar
        mSearchText = (EditText) findViewById(R.id.search_edit_text);
        mSearchText.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    search(null);
                    return true;
                }
                return false;
            }
        });

        // initializes the search results list view
        ListView searchResultListView = (ListView) findViewById(R.id.search_result_list_view);
        mSearchResultListAdapter = new SearchResultListAdapter(this, mSettingsManager);
        searchResultListView.setAdapter(mSearchResultListAdapter);
        searchResultListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final TranslationReader.SearchResult result = mData.results.get(position);
                getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE).edit()
                        .putInt(Constants.PREF_KEY_LAST_READ_BOOK, result.bookIndex)
                        .putInt(Constants.PREF_KEY_LAST_READ_CHAPTER, result.chapterIndex)
                        .putInt(Constants.PREF_KEY_LAST_READ_VERSE, result.verseIndex)
                        .commit();

                startActivity(new Intent(SearchActivity.this, TextActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            }
        });
    }

    private void populateUi() {
        mRootView.setBackgroundColor(mSettingsManager.backgroundColor());
        mSearchText.setTextColor(mSettingsManager.textColor());

        final String selected = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
                .getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
        if (!selected.equals(mData.selectedTranslationShortName)) {
            setTitle(selected);
            mData.selectedTranslationShortName = selected;

            mSearchText.setText(null);
            mSearchResultListAdapter.setSearchResults(null);
        } else {
            mSearchResultListAdapter.notifyDataSetChanged();
        }
    }

    public void search(View view) {
        final Editable searchToken = mSearchText.getText();
        if (searchToken.length() == 0)
            return;

        new AsyncTask<String, Void, List<TranslationReader.SearchResult>>() {
            protected void onPreExecute() {
                // running in the main thread

                mSearchResultListAdapter.setSearchResults(null);

                InputMethodManager inputManager
                        = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputManager != null) {
                    inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                }

                mProgressDialog = new ProgressDialog(SearchActivity.this);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setMessage(getText(R.string.progress_dialog_verses_searching));
                mProgressDialog.show();
            }

            protected List<TranslationReader.SearchResult> doInBackground(String... params) {
                // running in the worker thread

                mTranslationReader.selectTranslation(
                        getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
                                .getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null));
                return mTranslationReader.search(params[0]);
            }

            protected void onPostExecute(List<TranslationReader.SearchResult> results) {
                // running in the main thread

                mData.results = results;
                mSearchResultListAdapter.setSearchResults(results);
                mProgressDialog.dismiss();

                String text = getResources().getString(R.string.toast_verses_searched,
                        results == null ? 0 : results.size());
                Toast.makeText(SearchActivity.this, text, Toast.LENGTH_SHORT).show();
            }

            private ProgressDialog mProgressDialog;
        }.execute(searchToken.toString());
    }

    private static class NonConfigurationData {
        Editable searchText;
        List<TranslationReader.SearchResult> results;
        String selectedTranslationShortName;
    }

    private EditText mSearchText;
    private View mRootView;

    private NonConfigurationData mData;

    private SearchResultListAdapter mSearchResultListAdapter;
    private SettingsManager mSettingsManager;
    private TranslationReader mTranslationReader;
}
