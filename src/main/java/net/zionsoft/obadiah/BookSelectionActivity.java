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

package net.zionsoft.obadiah;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.ReadingProgressManager;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.appindexing.AppIndexingManager;
import net.zionsoft.obadiah.ui.activities.ReadingProgressActivity;
import net.zionsoft.obadiah.ui.activities.SearchActivity;
import net.zionsoft.obadiah.ui.activities.SettingsActivity;
import net.zionsoft.obadiah.ui.activities.TranslationManagementActivity;
import net.zionsoft.obadiah.ui.fragments.ChapterSelectionFragment;
import net.zionsoft.obadiah.ui.fragments.TextFragment;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.ArrayList;
import java.util.List;

public class BookSelectionActivity extends ActionBarActivity
        implements ChapterSelectionFragment.Listener, TextFragment.Listener {
    private AppIndexingManager mAppIndexingManager;
    private Bible mBible;
    private Settings mSettings;
    private SharedPreferences mPreferences;

    private String mCurrentTranslation;
    private List<String> mBookNames;
    private int mCurrentBook;
    private int mCurrentChapter;

    private ChapterSelectionFragment mChapterSelectionFragment;
    private TextFragment mTextFragment;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private Spinner mTranslationsSpinner;
    private View mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAppIndexingManager = new AppIndexingManager(this);
        mBible = Bible.getInstance();
        mSettings = Settings.getInstance();
        mPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);

        initializeUi();
        checkDeepLink();
    }

    private void initializeUi() {
        setContentView(R.layout.activity_book_selection);

        mRootView = getWindow().getDecorView();

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, 0, 0);
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        final FragmentManager fm = getSupportFragmentManager();
        mChapterSelectionFragment = (ChapterSelectionFragment) fm.findFragmentById(R.id.left_drawer);
        mTextFragment = (TextFragment) fm.findFragmentById(R.id.text_fragment);
    }

    private void checkDeepLink() {
        final Uri uri = getIntent().getData();
        if (uri == null)
            return;

        // format: /bible/<translation-short-name>/<book-index>/<chapter-index>
        final String[] parts = uri.getPath().split("/");
        if (parts.length < 5)
            return;

        try {
            final String translationShortName = parts[2];
            final int bookIndex = Integer.parseInt(parts[3]);
            final int chapterIndex = Integer.parseInt(parts[4]);
            mPreferences.edit()
                    .putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, translationShortName)
                    .putInt(Constants.PREF_KEY_LAST_READ_BOOK, bookIndex)
                    .putInt(Constants.PREF_KEY_LAST_READ_CHAPTER, chapterIndex)
                    .putInt(Constants.PREF_KEY_LAST_READ_VERSE, 0)
                    .apply();
        } catch (Exception e) {
            Analytics.trackException("Invalid URI: " + uri.toString());
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mDrawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mAppIndexingManager.onStart();
        populateUi();
    }

    private void populateUi() {
        mSettings.refresh();
        mRootView.setKeepScreenOn(mSettings.keepScreenOn());
        mRootView.setBackgroundColor(mSettings.getBackgroundColor());

        mCurrentTranslation = mPreferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
        if (mCurrentTranslation == null) {
            // TODO before 1.7.0, there is a bug that set last read translation
            DialogHelper.showDialog(this, false, R.string.dialog_no_translation,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(BookSelectionActivity.this, TranslationManagementActivity.class));
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

        mCurrentBook = mPreferences.getInt(Constants.PREF_KEY_LAST_READ_BOOK, 0);
        mCurrentChapter = mPreferences.getInt(Constants.PREF_KEY_LAST_READ_CHAPTER, 0);

        loadTranslations();
        loadTexts();
    }

    private void loadTranslations() {
        if (mTranslationsSpinner == null)
            return;

        mBible.loadDownloadedTranslations(new Bible.OnStringsLoadedListener() {
            @Override
            public void onStringsLoaded(List<String> strings) {
                if (strings == null || strings.size() == 0) {
                    if (mCurrentTranslation != null) {
                        DialogHelper.showDialog(BookSelectionActivity.this, false, R.string.dialog_retry,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        loadTranslations();
                                    }
                                }, null
                        );
                    }
                    return;
                }

                final int translationsCount = strings.size();
                final List<String> translations = new ArrayList<String>(translationsCount + 1);
                translations.addAll(strings);
                translations.add(getResources().getString(R.string.text_translations));
                mTranslationsSpinner.setAdapter(new ArrayAdapter<String>(BookSelectionActivity.this,
                        R.layout.item_drop_down, translations));

                int selected = 0;
                for (String translation : strings) {
                    if (translation.equals(mCurrentTranslation))
                        break;
                    ++selected;
                }
                mTranslationsSpinner.setSelection(selected);
                mTranslationsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (position == translationsCount) {
                            startActivity(new Intent(BookSelectionActivity.this, TranslationManagementActivity.class));
                            return;
                        }

                        final String selected = translations.get(position);
                        if (selected.equals(mCurrentTranslation))
                            return;

                        mCurrentTranslation = selected;
                        mPreferences.edit()
                                .putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, selected)
                                .putInt(Constants.PREF_KEY_LAST_READ_VERSE, mTextFragment.getCurrentVerse())
                                .apply();

                        loadTexts();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // do nothing
                    }
                });
            }
        });
    }

    private void loadTexts() {
        mBible.loadBookNames(mCurrentTranslation, new Bible.OnStringsLoadedListener() {
                    @Override
                    public void onStringsLoaded(List<String> strings) {
                        if (strings == null || strings.size() == 0) {
                            if (mCurrentTranslation != null) {
                                DialogHelper.showDialog(BookSelectionActivity.this, false, R.string.dialog_retry,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                loadTexts();
                                            }
                                        }, null
                                );
                            }
                            return;
                        }

                        mBookNames = strings;
                        updateTitle();
                    }
                }
        );

        mChapterSelectionFragment.setSelected(mCurrentTranslation, mCurrentBook, mCurrentChapter);

        mTextFragment.setSelected(mCurrentTranslation, mCurrentBook, mCurrentChapter,
                mPreferences.getInt(Constants.PREF_KEY_LAST_READ_VERSE, 0));
    }

    private void updateTitle() {
        final String bookName = mBookNames.get(mCurrentBook);
        setTitle(String.format("%s, %d", bookName, mCurrentChapter + 1));
        mAppIndexingManager.onView(mCurrentTranslation, bookName, mCurrentBook, mCurrentChapter);

        // TODO get an improved tracking algorithm, e.g. only consider as "read" if the user stays for a while
        ReadingProgressManager.getInstance().trackChapterReading(mCurrentBook, mCurrentChapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Analytics.trackScreen(BookSelectionActivity.class.getSimpleName());
    }

    @Override
    protected void onStop() {
        mPreferences.edit()
                .putInt(Constants.PREF_KEY_LAST_READ_BOOK, mCurrentBook)
                .putInt(Constants.PREF_KEY_LAST_READ_CHAPTER, mCurrentChapter)
                .putInt(Constants.PREF_KEY_LAST_READ_VERSE, mTextFragment.getCurrentVerse())
                .apply();
        mAppIndexingManager.onStop();

        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bookselection, menu);

        mTranslationsSpinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.action_translations));
        loadTranslations();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;

        switch (item.getItemId()) {
            case R.id.action_search:
                startActivity(new Intent(this, SearchActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            case R.id.action_reading_progress:
                startActivity(new Intent(this, ReadingProgressActivity.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onChapterSelected(int bookIndex, int chapterIndex) {
        if (mCurrentBook == bookIndex && mCurrentChapter == chapterIndex)
            return;
        mCurrentBook = bookIndex;
        mCurrentChapter = chapterIndex;

        mDrawerLayout.closeDrawers();
        mTextFragment.setSelected(bookIndex, chapterIndex, 0);
        updateTitle();
    }

    @Override
    public void onChapterSelected(int chapterIndex) {
        if (mCurrentChapter == chapterIndex)
            return;
        mCurrentChapter = chapterIndex;

        mChapterSelectionFragment.setSelected(mCurrentBook, mCurrentChapter);
        updateTitle();
    }
}
