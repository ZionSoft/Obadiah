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

package net.zionsoft.obadiah;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.Spinner;

import net.zionsoft.obadiah.injection.components.fragments.BibleReadingComponentFragment;
import net.zionsoft.obadiah.injection.scopes.ActivityScope;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.appindexing.AppIndexingManager;
import net.zionsoft.obadiah.model.utils.AppUpdateChecker;
import net.zionsoft.obadiah.model.utils.UriHelper;
import net.zionsoft.obadiah.mvp.presenters.BibleReadingPresenter;
import net.zionsoft.obadiah.mvp.views.BibleReadingView;
import net.zionsoft.obadiah.ui.activities.BaseAppCompatActivity;
import net.zionsoft.obadiah.ui.activities.ReadingProgressActivity;
import net.zionsoft.obadiah.ui.activities.SearchActivity;
import net.zionsoft.obadiah.ui.activities.SettingsActivity;
import net.zionsoft.obadiah.ui.activities.TranslationManagementActivity;
import net.zionsoft.obadiah.ui.adapters.BookExpandableListAdapter;
import net.zionsoft.obadiah.ui.fragments.TextFragment;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;

public class BookSelectionActivity extends BaseAppCompatActivity implements BibleReadingView,
        TextFragment.Listener, AdapterView.OnItemSelectedListener {
    private static final String KEY_MESSAGE_TYPE = "net.zionsoft.obadiah.BookSelectionActivity.KEY_MESSAGE_TYPE";
    private static final String KEY_BOOK_INDEX = "net.zionsoft.obadiah.BookSelectionActivity.KEY_BOOK_INDEX";
    private static final String KEY_CHAPTER_INDEX = "net.zionsoft.obadiah.BookSelectionActivity.KEY_CHAPTER_INDEX";
    private static final String KEY_VERSE_INDEX = "net.zionsoft.obadiah.BookSelectionActivity.KEY_VERSE_INDEX";

    public static Intent newStartReorderToTopIntent(Context context, String messageType,
                                                    int book, int chapter, int verse) {
        return newStartReorderToTopIntent(context)
                .putExtra(KEY_MESSAGE_TYPE, messageType)
                .putExtra(KEY_BOOK_INDEX, book)
                .putExtra(KEY_CHAPTER_INDEX, chapter)
                .putExtra(KEY_VERSE_INDEX, verse);
    }

    public static Intent newStartReorderToTopIntent(Context context) {
        final Intent startIntent = newStartIntent(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // there's some horrible issue with FLAG_ACTIVITY_REORDER_TO_FRONT for KitKat and above
            // ref. https://code.google.com/p/android/issues/detail?id=63570
            startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }
        return startIntent;
    }

    public static Intent newStartIntent(Context context) {
        return new Intent(context, BookSelectionActivity.class);
    }

    @ActivityScope
    @Inject
    BibleReadingPresenter bibleReadingPresenter;

    @Inject
    Settings settings;

    @Bind(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @Bind(R.id.book_list)
    ExpandableListView bookList;

    private AppIndexingManager appIndexingManager;

    private String currentTranslation;
    private List<String> bookNames;
    private int currentBook;
    private int currentChapter;

    private BookExpandableListAdapter bookListAdapter;
    private int lastExpandedGroup;

    private TextFragment textFragment;

    private ActionBarDrawerToggle drawerToggle;
    private Spinner translationsSpinner;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(BibleReadingComponentFragment.FRAGMENT_TAG) == null) {
            fm.beginTransaction()
                    .add(BibleReadingComponentFragment.newInstance(),
                            BibleReadingComponentFragment.FRAGMENT_TAG)
                    .commit();
        }

        appIndexingManager = new AppIndexingManager(this);

        initializeUi();
        checkDeepLink();
    }

    private void initializeUi() {
        setContentView(R.layout.activity_book_selection);

        rootView = getWindow().getDecorView();

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, 0, 0);
        drawerLayout.setDrawerListener(drawerToggle);

        bookListAdapter = new BookExpandableListAdapter(this,
                new BookExpandableListAdapter.OnChapterClickListener() {
                    @Override
                    public void onChapterClicked(int book, int chapter) {
                        if (currentBook == book && currentChapter == chapter) {
                            return;
                        }

                        currentBook = book;
                        currentChapter = chapter;

                        bookListAdapter.setSelected(currentBook, currentChapter);
                        bookListAdapter.notifyDataSetChanged();

                        drawerLayout.closeDrawers();
                        textFragment.setSelected(currentBook, currentChapter, 0);

                        updateTitle();
                    }
                });
        bookList.setAdapter(bookListAdapter);
        bookList.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                parent.smoothScrollToPosition(groupPosition);
                if (parent.isGroupExpanded(groupPosition)) {
                    parent.collapseGroup(groupPosition);
                } else {
                    parent.expandGroup(groupPosition);
                    if (lastExpandedGroup != groupPosition) {
                        parent.collapseGroup(lastExpandedGroup);
                        lastExpandedGroup = groupPosition;
                    }
                }
                return true;
            }
        });

        final FragmentManager fm = getSupportFragmentManager();
        textFragment = (TextFragment) fm.findFragmentById(R.id.text_fragment);
    }

    private void checkDeepLink() {
        final Intent startIntent = getIntent();
        final Uri uri = startIntent.getData();
        if (uri != null) {
            UriHelper.checkDeepLink(this, uri);
        } else {
            final String messageType = startIntent.getStringExtra(KEY_MESSAGE_TYPE);
            if (TextUtils.isEmpty(messageType)) {
                return;
            }
            final int bookIndex = startIntent.getIntExtra(KEY_BOOK_INDEX, -1);
            if (bookIndex < 0) {
                return;
            }
            final int chapterIndex = startIntent.getIntExtra(KEY_CHAPTER_INDEX, -1);
            if (chapterIndex < 0) {
                return;
            }
            final int verseIndex = startIntent.getIntExtra(KEY_VERSE_INDEX, -1);
            if (verseIndex < 0) {
                return;
            }

            bibleReadingPresenter.storeReadingProgress(bookIndex, chapterIndex, verseIndex);

            Analytics.trackNotificationEvent("notification_opened", messageType);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        if (fragment instanceof BibleReadingComponentFragment) {
            ((BibleReadingComponentFragment) fragment).getComponent().inject(this);

            final View rootView = getWindow().getDecorView();
            rootView.setKeepScreenOn(settings.keepScreenOn());
            rootView.setBackgroundColor(settings.getBackgroundColor());
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        drawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();

        appIndexingManager.onStart();
        populateUi();
        showUpdateDialog();
    }

    private void showUpdateDialog() {
        if (!bibleReadingPresenter.hasDownloadedTranslation()) {
            // do nothing if there's no translation installed (most likely it's the 1st time use)
            return;
        }

        if (AppUpdateChecker.shouldUpdate(this)) {
            DialogHelper.showDialog(BookSelectionActivity.this, false,
                    R.string.dialog_new_version_available_message,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW).setData(Constants.GOOGLE_PLAY_URI));
                                Analytics.trackUIEvent("upgrade_app");
                            } catch (ActivityNotFoundException e) {
                                Analytics.trackException("Failed to open market for updating: "
                                        + Build.MANUFACTURER + ", " + Build.MODEL);

                                // falls back to open a link in browser
                                startActivity(new Intent(Intent.ACTION_VIEW).setData(
                                        Uri.parse("http://www.zionsoft.net/bible-reader/")));
                            }
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Analytics.trackUIEvent("ignore_upgrade_app");
                        }
                    });
            AppUpdateChecker.markAsUpdateAsked(this);
        }
    }

    private void populateUi() {
        settings.refresh();
        rootView.setKeepScreenOn(settings.keepScreenOn());
        rootView.setBackgroundColor(settings.getBackgroundColor());

        currentTranslation = bibleReadingPresenter.loadCurrentTranslation();
        if (TextUtils.isEmpty(currentTranslation)) {
            return;
        }

        currentBook = bibleReadingPresenter.loadCurrentBook();
        currentChapter = bibleReadingPresenter.loadCurrentChapter();
    }

    private void updateTitle() {
        final String bookName = bookNames.get(currentBook);
        setTitle(String.format("%s, %d", bookName, currentChapter + 1));
        appIndexingManager.onView(currentTranslation, bookName, currentBook, currentChapter);

        // TODO get an improved tracking algorithm, e.g. only consider as "read" if the user stays for a while
        bibleReadingPresenter.trackReadingProgress(currentBook, currentChapter);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        bibleReadingPresenter.takeView(this);
        loadTranslations();
    }

    private void loadTranslations() {
        // we should wait until the menu is inflated
        if (translationsSpinner != null) {
            bibleReadingPresenter.loadTranslations();
        }
    }

    @Override
    protected void onPause() {
        bibleReadingPresenter.dropView();
        super.onPause();
    }

    @Override
    protected void onStop() {
        bibleReadingPresenter.storeReadingProgress(currentBook, currentChapter, textFragment.getCurrentVerse());
        appIndexingManager.onStop();

        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bookselection, menu);

        translationsSpinner = (Spinner) MenuItemCompat.getActionView(menu.findItem(R.id.action_translations));
        loadTranslations();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item))
            return true;

        switch (item.getItemId()) {
            case R.id.action_search:
                AnimationHelper.slideIn(this, SearchActivity.newStartReorderToTopIntent(this));
                return true;
            case R.id.action_reading_progress:
                AnimationHelper.slideIn(this, ReadingProgressActivity.newStartIntent(this));
                return true;
            case R.id.action_settings:
                AnimationHelper.slideIn(this, SettingsActivity.newStartIntent(this));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onChapterSelected(int chapterIndex) {
        if (currentChapter == chapterIndex) {
            return;
        }
        currentChapter = chapterIndex;

        updateBookList();
        updateTitle();
    }

    private void updateBookList() {
        bookListAdapter.setSelected(currentBook, currentChapter);
        bookListAdapter.notifyDataSetChanged();

        lastExpandedGroup = currentBook;
        bookList.expandGroup(currentBook);
        bookList.setSelectedGroup(currentBook);
    }

    @Override
    public void onTranslationsLoaded(List<String> translations) {
        final int translationsCount = translations.size();
        int selected;
        for (selected = 0; selected < translationsCount; ++selected) {
            if (translations.get(selected).equals(currentTranslation)) {
                break;
            }
        }
        if (selected == translationsCount) {
            // the requested translation is not available, use the first one in the list
            selected = 0;
            currentTranslation = translations.get(0);
            bibleReadingPresenter.setCurrentTranslation(currentTranslation);
        }

        // appends "More" to end of list that is to be shown in spinner
        final List<String> names = new ArrayList<>(translationsCount + 1);
        names.addAll(translations);
        names.add(getString(R.string.text_more_translations));
        translationsSpinner.setAdapter(new ArrayAdapter<>(
                getSupportActionBar().getThemedContext(), R.layout.item_drop_down, names));
        translationsSpinner.setSelection(selected);
        translationsSpinner.setOnItemSelectedListener(this);

        loadTexts();
    }

    private void loadTexts() {
        bibleReadingPresenter.loadBookNames(currentTranslation);
        textFragment.setSelected(currentTranslation, currentBook, currentChapter,
                bibleReadingPresenter.loadCurrentVerse());
    }

    @Override
    public void onTranslationsLoadFailed() {
        DialogHelper.showDialog(this, false, R.string.dialog_retry,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        bibleReadingPresenter.loadTranslations();
                    }
                }, null);
    }

    @Override
    public void onNoTranslationAvailable() {
        DialogHelper.showDialog(this, false, R.string.dialog_no_translation,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AnimationHelper.slideIn(BookSelectionActivity.this,
                                TranslationManagementActivity.newStartIntent(BookSelectionActivity.this));
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }
        );
    }

    @Override
    public void onBookNamesLoaded(List<String> bookNames) {
        this.bookNames = bookNames;
        updateTitle();

        bookListAdapter.setBookNames(bookNames);
        updateBookList();
    }

    @Override
    public void onBookNamesLoadFailed() {
        if (currentTranslation != null) {
            DialogHelper.showDialog(this, false, R.string.dialog_retry,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loadTexts();
                        }
                    }, null);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final Adapter adapter = parent.getAdapter();
        if (position == adapter.getCount() - 1) {
            // last item ("More") selected, opens the translation management activity
            AnimationHelper.slideIn(BookSelectionActivity.this,
                    TranslationManagementActivity.newStartIntent(BookSelectionActivity.this));
            return;
        }

        final String selected = (String) adapter.getItem(position);
        if (selected == null || selected.equals(currentTranslation)) {
            return;
        }

        Analytics.trackTranslationSelection(selected);
        currentTranslation = selected;
        bibleReadingPresenter.storeReadingProgress(currentBook, currentChapter, textFragment.getCurrentVerse());

        loadTexts();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }
}
