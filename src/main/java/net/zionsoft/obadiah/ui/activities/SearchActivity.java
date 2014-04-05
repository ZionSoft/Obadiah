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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
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

import net.zionsoft.obadiah.BookSelectionActivity;
import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Analytics;
import net.zionsoft.obadiah.model.Obadiah;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.Verse;
import net.zionsoft.obadiah.ui.adapters.SearchResultListAdapter;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

public class SearchActivity extends ActionBarActivity {
    private static class NonConfigurationData {
        Editable searchText;
        String currentTranslation;
        List<Verse> verses;
    }

    private NonConfigurationData mData;

    private Settings mSettings;
    private SharedPreferences mPreferences;

    private SearchResultListAdapter mSearchResultListAdapter;

    private View mRootView;
    private EditText mSearchText;
    private View mLoadingSpinner;
    private ListView mSearchResultListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = Settings.getInstance();
        mPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);

        initializeUi();

        mData = (NonConfigurationData) getLastCustomNonConfigurationInstance();
        if (mData == null) {
            mData = new NonConfigurationData();
        } else {
            mSearchText.setText(mData.searchText);
            mSearchResultListAdapter.setVerses(mData.verses);
        }
    }

    private void initializeUi() {
        setContentView(R.layout.activity_search);

        mRootView = getWindow().getDecorView();

        mSearchText = (EditText) findViewById(R.id.search_edit_text);
        mSearchText.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    onSearchClicked(null);
                    return true;
                }
                return false;
            }
        });

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

                startActivity(new Intent(SearchActivity.this, BookSelectionActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        populateUi();
    }

    private void populateUi() {
        mSettings.refresh();
        mRootView.setBackgroundColor(mSettings.getBackgroundColor());
        mSearchText.setTextColor(mSettings.getTextColor());
        mSearchText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSettings.getTextSize());

        final String selected = mPreferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
        setTitle(selected);
        if (!selected.equals(mData.currentTranslation)) {
            mData.currentTranslation = selected;

            mSearchText.setText(null);
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
    public Object onRetainCustomNonConfigurationInstance() {
        mData.searchText = mSearchText.getText();
        return mData;
    }

    public void onSearchClicked(View view) {
        final Editable searchToken = mSearchText.getText();
        if (TextUtils.isEmpty(searchToken))
            return;

        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        search(searchToken.toString());
    }

    private void search(final String keyword) {
        mLoadingSpinner.setVisibility(View.VISIBLE);
        mSearchResultListView.setVisibility(View.GONE);

        Obadiah.getInstance().searchVerses(mData.currentTranslation, keyword,
                new Obadiah.OnVersesSearchedListener() {
                    @Override
                    public void onVersesSearched(List<Verse> verses) {
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

                        mData.verses = verses;

                        String text = getResources().getString(R.string.toast_verses_searched, verses.size());
                        Toast.makeText(SearchActivity.this, text, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}
