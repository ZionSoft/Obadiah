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
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import net.zionsoft.obadiah.model.Analytics;
import net.zionsoft.obadiah.model.Obadiah;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.ui.activities.SearchActivity;
import net.zionsoft.obadiah.ui.activities.SettingsActivity;
import net.zionsoft.obadiah.ui.activities.TranslationManagementActivity;
import net.zionsoft.obadiah.ui.fragments.ChapterSelectionFragment;
import net.zionsoft.obadiah.ui.fragments.TextFragment;
import net.zionsoft.obadiah.ui.utils.DialogHelper;
import net.zionsoft.obadiah.ui.utils.UIHelper;

import java.util.List;

public class BookSelectionActivity extends ActionBarActivity
        implements ChapterSelectionFragment.Listener, TextFragment.Listener {
    private Obadiah mObadiah;
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
    private TextView mBookNameTextView;
    private View mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mObadiah = Obadiah.getInstance();
        mSettings = Settings.getInstance();
        mPreferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);

        upgrade();
        initializeUi();
    }

    private void upgrade() {
        final int version = mPreferences.getInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, 0);
        final int applicationVersion;
        try {
            //noinspection ConstantConditions
            applicationVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never reach here
            return;
        }
        if (version >= applicationVersion)
            return;

        if (version < 10500) {
            // TODO remove everything in getFilesDir()
            mPreferences.edit()
                    .remove("selectedTranslation")
                    .putInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, 10500)
                    .apply();
        }
        if (version < 10700) {
            mPreferences.edit()
                    .remove("lastUpdated")
                    .putInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, 10700)
                    .apply();
        }
        if (version < 10800) {
            mPreferences.edit()
                    .remove("PREF_KEY_DOWNLOADING_TRANSLATION").remove("PREF_KEY_DOWNLOADING_TRANSLATION_LIST")
                    .remove("PREF_KEY_REMOVING_TRANSLATION").remove("PREF_KEY_UPGRADING")
                    .putInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, 10800)
                    .apply();
        }
        mPreferences.edit()
                .putInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, applicationVersion)
                .apply();
    }

    private void initializeUi() {
        UIHelper.forceActionBarOverflowMenu(this);
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mDrawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();

        populateUi();
    }

    private void populateUi() {
        mSettings.refresh();
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
        mObadiah.loadDownloadedTranslations(new Obadiah.OnStringsLoadedListener() {
            @Override
            public void onStringsLoaded(List<String> strings) {
                if (strings == null || strings.size() == 0) {
                    DialogHelper.showDialog(BookSelectionActivity.this, false, R.string.dialog_retry,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    loadTranslations();
                                }
                            }, null
                    );
                    return;
                }

                final ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

                final List<String> translations = strings;
                actionBar.setListNavigationCallbacks(
                        new ArrayAdapter<String>(BookSelectionActivity.this, R.layout.item_drop_down, translations),
                        new ActionBar.OnNavigationListener() {
                            @Override
                            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                                final String selected = translations.get(itemPosition);
                                if (!selected.equals(mCurrentTranslation)) {
                                    Analytics.trackTranslationSelection(selected);

                                    mCurrentTranslation = selected;
                                    mPreferences.edit()
                                            .putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, selected)
                                            .putInt(Constants.PREF_KEY_LAST_READ_VERSE, mTextFragment.getCurrentVerse())
                                            .apply();

                                    loadTexts();
                                }
                                return true;
                            }
                        }
                );
                int i = 0;
                for (String translation : translations) {
                    if (translation.equals(mCurrentTranslation)) {
                        actionBar.setSelectedNavigationItem(i);
                        break;
                    }
                    ++i;
                }
            }
        });
    }

    private void loadTexts() {
        mObadiah.loadBookNames(mCurrentTranslation, new Obadiah.OnStringsLoadedListener() {
                    @Override
                    public void onStringsLoaded(List<String> strings) {
                        if (strings == null || strings.size() == 0) {
                            DialogHelper.showDialog(BookSelectionActivity.this, false, R.string.dialog_retry,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            loadTexts();
                                        }
                                    }, null
                            );
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
        mBookNameTextView = (TextView) MenuItemCompat.getActionView(menu.findItem(R.id.action_book_name));
        updateTitle();

        return true;
    }

    private void updateTitle() {
        // onCreateOptionsMenu() is invoked after onResume(), but we can't guarantee the loading of
        // book names is finished before that
        if (mBookNames != null && mBookNameTextView != null)
            mBookNameTextView.setText(String.format("%s, %d", mBookNames.get(mCurrentBook), mCurrentChapter + 1));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;

        switch (item.getItemId()) {
            case R.id.action_search:
                startActivity(new Intent(this, SearchActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_manage_translation:
                startActivity(new Intent(this, TranslationManagementActivity.class));
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
