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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ListView;

import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.bible.TranslationManager;
import net.zionsoft.obadiah.bible.TranslationReader;
import net.zionsoft.obadiah.support.UpgradeAsyncTask;
import net.zionsoft.obadiah.util.SettingsManager;

public class BookSelectionActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bookselection_activity);

        // convert to new format from old format if needed
        final int currentApplicationVersion
                = getSharedPreferences(Constants.SETTING_KEY, MODE_PRIVATE)
                .getInt(Constants.CURRENT_APPLICATION_VERSION_SETTING_KEY, 0);
        if (currentApplicationVersion < Constants.CURRENT_APPLICATION_VERSION) {
            mUpgrading = true;
            new UpgradeAsyncTask(this).execute();
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mSettingsManager = new SettingsManager(this);
        mTranslationManager = new TranslationManager(this);
        mTranslationReader = new TranslationReader(this);

        // initializes views
        mLoadingSpinner = findViewById(R.id.book_selection_loading_spinner);
        mMainView = findViewById(R.id.book_selection_main_view);

        // initializes the book list view
        mBookListView = (ListView) findViewById(R.id.book_listview);
        mBookListAdapter = new BookListAdapter(this, mSettingsManager);
        mBookListView.setAdapter(mBookListAdapter);
        mBookListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mSelectedBook == position)
                    return;
                mSelectedBook = position;
                updateUi();
            }
        });

        // initializes the chapters list view
        mChaptersGridView = (GridView) findViewById(R.id.chapter_gridview);
        mChapterListAdapter = new ChapterListAdapter(this, mSettingsManager);
        mChaptersGridView.setAdapter(mChapterListAdapter);
        mChaptersGridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mLastReadBook != mSelectedBook || mLastReadChapter != position) {
                    getSharedPreferences(Constants.SETTING_KEY, MODE_PRIVATE).edit()
                            .putInt(Constants.CURRENT_BOOK_SETTING_KEY, mSelectedBook)
                            .putInt(Constants.CURRENT_CHAPTER_SETTING_KEY, position)
                            .putInt(Constants.CURRENT_VERSE_SETTING_KEY, 0)
                            .commit();
                }

                startActivity(new Intent(BookSelectionActivity.this, TextActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSettingsManager.refresh();
        final int backgroundColor = mSettingsManager.backgroundColor();
        mBookListView.setBackgroundColor(backgroundColor);
        mBookListView.setCacheColorHint(backgroundColor);
        mChaptersGridView.setBackgroundColor(backgroundColor);
        mChaptersGridView.setCacheColorHint(backgroundColor);

        if (mUpgrading)
            return;

        populateUi();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bookselection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_select_translation:
                startActivity(new Intent(this, TranslationSelectionActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onUpgradeFinished() {
        populateUi();
        mUpgrading = false;
    }

    private void populateUi() {
        boolean hasInstalledTranslation = false;
        final TranslationInfo[] translations = mTranslationManager.translations();
        if (translations != null) {
            for (TranslationInfo translationInfo : translations) {
                if (translationInfo.installed) {
                    hasInstalledTranslation = true;
                    break;
                }
            }
        }

        if (hasInstalledTranslation) {
            loadLastReadTranslation();
        } else {
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(R.string.text_no_translation).setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            startActivity(new Intent(BookSelectionActivity.this,
                                    TranslationSelectionActivity.class));
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    BookSelectionActivity.this.finish();
                }
            });
            alertDialogBuilder.create().show();
        }
    }

    private void loadLastReadTranslation() {
        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected void onPreExecute() {
                mLoadingSpinner.setVisibility(View.VISIBLE);
                mMainView.setVisibility(View.GONE);
            }

            @Override
            protected String[] doInBackground(Void... params) {
                // loads last read translation, book, and chapter
                final SharedPreferences preferences
                        = getSharedPreferences(Constants.SETTING_KEY, MODE_PRIVATE);
                mTranslationReader.selectTranslation(preferences
                        .getString(Constants.CURRENT_TRANSLATION_SETTING_KEY, null));
                mLastReadBook = preferences.getInt(Constants.CURRENT_BOOK_SETTING_KEY, -1);
                mLastReadChapter
                        = preferences.getInt(Constants.CURRENT_CHAPTER_SETTING_KEY, -1);

                mSelectedBook = mLastReadBook < 0 ? 0 : mLastReadBook;

                return mTranslationReader.bookNames();
            }

            @Override
            protected void onPostExecute(String[] bookNames) {
                Animator.fadeOut(mLoadingSpinner);
                Animator.fadeIn(mMainView);

                // updates book list adapter and chapter list adapter
                mBookListAdapter.setTexts(bookNames);
                mChapterListAdapter.setLastReadChapter(mLastReadBook, mLastReadChapter);

                updateUi();

                // scrolls to the currently selected book
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
                    mBookListView.smoothScrollToPosition(mSelectedBook);
                else
                    mBookListView.setSelection(mSelectedBook);
            }
        }.execute();
    }

    private void updateUi() {
        // format: <translation short name> - <book name>
        setTitle(String.format("%s - %s", mTranslationReader.selectedTranslationShortName(),
                mTranslationReader.bookNames()[mSelectedBook]));

        mBookListAdapter.selectBook(mSelectedBook);
        mChapterListAdapter.selectBook(mSelectedBook);
        mChaptersGridView.setSelection(0);
    }

    private boolean mUpgrading = false;

    private int mLastReadBook;
    private int mLastReadChapter;
    private int mSelectedBook = -1;

    private BookListAdapter mBookListAdapter;
    private ChapterListAdapter mChapterListAdapter;

    private GridView mChaptersGridView;
    private ListView mBookListView;
    private View mLoadingSpinner;
    private View mMainView;

    private SettingsManager mSettingsManager;
    private TranslationManager mTranslationManager;
    private TranslationReader mTranslationReader;
}
