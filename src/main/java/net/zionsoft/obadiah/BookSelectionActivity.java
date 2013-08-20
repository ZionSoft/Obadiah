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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

import net.zionsoft.obadiah.bible.TranslationReader;
import net.zionsoft.obadiah.support.UpgradeService;
import net.zionsoft.obadiah.util.SettingsManager;

public class BookSelectionActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book_selection_activity);

        if (needsUpgrade())
            upgrade();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mSettingsManager = new SettingsManager(this);
        mTranslationReader = new TranslationReader(this);

        initializeUi();
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

        if (isUpgrading())
            return;

        populateUi();
    }

    @Override
    protected void onDestroy() {
        unregisterUpgradeStatusListener();

        super.onDestroy();
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


    // upgrades from an older version

    private boolean needsUpgrade() {
        return getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
                .getInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, 0)
                < Constants.CURRENT_APPLICATION_VERSION;
    }

    private boolean isUpgrading() {
        return getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(Constants.PREF_KEY_UPGRADING, false);
    }

    private void upgrade() {
        registerUpgradeStatusListener();

        if (!isUpgrading()) {
            getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putBoolean(Constants.PREF_KEY_UPGRADING, true).commit();
            startService(new Intent(this, UpgradeService.class));
        }
    }

    private void registerUpgradeStatusListener() {
        if (mUpgradeStatusListener != null)
            return;

        mUpgradeStatusListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterUpgradeStatusListener();

                final int status = intent.getIntExtra(UpgradeService.KEY_STATUS,
                        UpgradeService.STATUS_SUCCESS);
                if (status == UpgradeService.STATUS_SUCCESS) {
                    populateUi();
                } else {
                    DialogHelper.showDialog(BookSelectionActivity.this, false,
                            status == UpgradeService.STATUS_NETWORK_FAILURE
                                    ? R.string.dialog_network_failure_message
                                    : R.string.dialog_initialization_failure_message,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    upgrade();
                                }
                            },
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            }
                    );
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mUpgradeStatusListener,
                new IntentFilter(UpgradeService.ACTION_STATUS_UPDATE));
    }

    private void unregisterUpgradeStatusListener() {
        if (mUpgradeStatusListener == null)
            return;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mUpgradeStatusListener);
        mUpgradeStatusListener = null;
    }


    // UI related

    private void initializeUi() {
        mLoadingSpinner = findViewById(R.id.book_selection_loading_spinner);
        mMainView = findViewById(R.id.book_selection_main_view);

        // book list view
        mBookListView = (ListView) findViewById(R.id.book_list_view);
        mBookListAdapter = new BookListAdapter(this, mSettingsManager);
        mBookListView.setAdapter(mBookListAdapter);
        mBookListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mSelectedBook == position)
                    return;
                mSelectedBook = position;
                updateUi();
            }
        });

        // chapters list view
        mChaptersGridView = (GridView) findViewById(R.id.chapter_grid_view);
        mChapterListAdapter = new ChapterListAdapter(this, mSettingsManager);
        mChaptersGridView.setAdapter(mChapterListAdapter);
        mChaptersGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mLastReadBook != mSelectedBook || mLastReadChapter != position) {
                    getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE).edit()
                            .putInt(Constants.PREF_KEY_LAST_READ_BOOK, mSelectedBook)
                            .putInt(Constants.PREF_KEY_LAST_READ_CHAPTER, position)
                            .putInt(Constants.PREF_KEY_LAST_READ_VERSE, 0)
                            .commit();
                }

                startActivity(new Intent(BookSelectionActivity.this, TextActivity.class));
            }
        });
    }

    private void populateUi() {
        final SharedPreferences preferences
                = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        final String lastReadTranslation
                = preferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
        if (lastReadTranslation == null) {
            // no translation installed
            DialogHelper.showDialog(this, false, R.string.dialog_no_translation_message,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivity(new Intent(BookSelectionActivity.this,
                                    TranslationDownloadActivity.class));
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    }
            );
            return;
        }

        mLastReadBook = preferences.getInt(Constants.PREF_KEY_LAST_READ_BOOK, -1);
        mLastReadChapter = preferences.getInt(Constants.PREF_KEY_LAST_READ_CHAPTER, -1);
        mSelectedBook = mLastReadBook < 0 ? 0 : mLastReadBook;

        if (lastReadTranslation.equals(mLastReadTranslation)) {
            updateUi();
            return;
        }

        mLastReadTranslation = lastReadTranslation;
        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected void onPreExecute() {
                mLoadingSpinner.setVisibility(View.VISIBLE);
                mMainView.setVisibility(View.GONE);
            }

            @Override
            protected String[] doInBackground(Void... params) {
                mTranslationReader.selectTranslation(mLastReadTranslation);
                return mTranslationReader.bookNames();
            }

            @Override
            protected void onPostExecute(String[] bookNames) {
                Animator.fadeOut(mLoadingSpinner);
                Animator.fadeIn(mMainView);

                mBookListAdapter.setTexts(bookNames);

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
        mChapterListAdapter.setLastReadChapter(mLastReadBook, mLastReadChapter);
        mChaptersGridView.setSelection(0);
    }

    private BroadcastReceiver mUpgradeStatusListener;

    private String mLastReadTranslation;
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
    private TranslationReader mTranslationReader;
}
