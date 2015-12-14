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

package net.zionsoft.obadiah.biblereading;

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
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.Toast;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.injection.scopes.ActivityScope;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.appindexing.AppIndexingManager;
import net.zionsoft.obadiah.utils.UriHelper;
import net.zionsoft.obadiah.ui.utils.BaseAppCompatActivity;
import net.zionsoft.obadiah.readingprogress.ReadingProgressActivity;
import net.zionsoft.obadiah.search.SearchActivity;
import net.zionsoft.obadiah.misc.settings.SettingsActivity;
import net.zionsoft.obadiah.translations.TranslationManagementActivity;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;

public class BibleReadingActivity extends BaseAppCompatActivity implements BibleReadingView,
        AdapterView.OnItemSelectedListener, BookExpandableListAdapter.OnChapterSelectedListener,
        ExpandableListView.OnGroupClickListener, VersePagerAdapter.Listener, ViewPager.OnPageChangeListener {
    private static final String KEY_MESSAGE_TYPE = "net.zionsoft.obadiah.biblereading.BibleReadingActivity.KEY_MESSAGE_TYPE";
    private static final String KEY_BOOK_INDEX = "net.zionsoft.obadiah.biblereading.BibleReadingActivity.KEY_BOOK_INDEX";
    private static final String KEY_CHAPTER_INDEX = "net.zionsoft.obadiah.biblereading.BibleReadingActivity.KEY_CHAPTER_INDEX";
    private static final String KEY_VERSE_INDEX = "net.zionsoft.obadiah.biblereading.BibleReadingActivity.KEY_VERSE_INDEX";

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
        return new Intent(context, BibleReadingActivity.class);
    }

    @ActivityScope
    @Inject
    BibleReadingPresenter bibleReadingPresenter;

    @Inject
    Settings settings;

    @Bind(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.book_list)
    ExpandableListView bookList;

    @Bind(R.id.verse_pager)
    ViewPager versePager;

    private AppIndexingManager appIndexingManager;

    private String currentTranslation;
    private List<String> bookNames;
    private int currentBook;
    private int currentChapter;

    private BookExpandableListAdapter bookListAdapter;
    private int lastExpandedGroup;

    private VersePagerAdapter versePagerAdapter;
    private ActionMode actionMode;
    @SuppressWarnings("deprecation")
    private ClipboardManager clipboardManager;

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

        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0);
        drawerLayout.setDrawerListener(drawerToggle);

        bookListAdapter = new BookExpandableListAdapter(this, this);
        bookList.setAdapter(bookListAdapter);
        bookList.setOnGroupClickListener(this);

        versePagerAdapter = new VersePagerAdapter(this, this);
        versePager.setAdapter(versePagerAdapter);
        versePager.addOnPageChangeListener(this);
    }

    private static String buildText(List<Verse> verses) {
        if (verses == null || verses.size() == 0)
            return null;

        // format: <book name> <chapter index>:<verse index> <verse text>
        final StringBuilder text = new StringBuilder();
        for (Verse verse : verses) {
            text.append(String.format("%S %d:%d %s\n", verse.bookName, verse.chapterIndex + 1,
                    verse.verseIndex + 1, verse.verseText));
        }
        return text.toString();
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
    }

    private void populateUi() {
        rootView.setKeepScreenOn(settings.keepScreenOn());
        rootView.setBackgroundColor(settings.getBackgroundColor());

        versePagerAdapter.notifyDataSetChanged();

        currentTranslation = bibleReadingPresenter.loadCurrentTranslation();
        if (TextUtils.isEmpty(currentTranslation)) {
            return;
        }

        currentBook = bibleReadingPresenter.loadCurrentBook();
        currentChapter = bibleReadingPresenter.loadCurrentChapter();
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
        bibleReadingPresenter.storeReadingProgress(currentBook, currentChapter,
                versePagerAdapter.getCurrentVerse(versePager.getCurrentItem()));
        appIndexingManager.onStop();

        if (actionMode != null) {
            actionMode.finish();
        }

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
        translationsSpinner.setAdapter(new ArrayAdapter<>(this, R.layout.item_drop_down, names));
        translationsSpinner.setSelection(selected);
        translationsSpinner.setOnItemSelectedListener(this);

        loadTexts();
    }

    private void loadTexts() {
        bibleReadingPresenter.loadBookNames(currentTranslation);

        versePagerAdapter.setTranslationShortName(currentTranslation);
        versePagerAdapter.setSelected(currentBook, currentChapter,
                bibleReadingPresenter.loadCurrentVerse());
        versePagerAdapter.notifyDataSetChanged();
        versePager.setCurrentItem(currentChapter, true);
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
                        AnimationHelper.slideIn(BibleReadingActivity.this,
                                TranslationManagementActivity.newStartIntent(BibleReadingActivity.this));
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

    private void updateTitle() {
        final String bookName = bookNames.get(currentBook);
        toolbar.setTitle(String.format("%s, %d", bookName, currentChapter + 1));
        appIndexingManager.onView(currentTranslation, bookName, currentBook, currentChapter);

        // TODO get an improved tracking algorithm, e.g. only consider as "read" if the user stays for a while
        bibleReadingPresenter.trackReadingProgress(currentBook, currentChapter);
    }

    private void updateBookList() {
        bookListAdapter.setSelected(currentBook, currentChapter);
        bookListAdapter.notifyDataSetChanged();

        lastExpandedGroup = currentBook;
        bookList.expandGroup(currentBook);
        bookList.setSelectedGroup(currentBook);
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
            AnimationHelper.slideIn(BibleReadingActivity.this,
                    TranslationManagementActivity.newStartIntent(BibleReadingActivity.this));
            return;
        }

        final String selected = (String) adapter.getItem(position);
        if (selected == null || selected.equals(currentTranslation)) {
            return;
        }

        Analytics.trackTranslationSelection(selected);
        currentTranslation = selected;
        bibleReadingPresenter.storeReadingProgress(currentBook, currentChapter,
                versePagerAdapter.getCurrentVerse(versePager.getCurrentItem()));

        loadTexts();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // do nothing
    }

    @Override
    public void onChapterSelected(int book, int chapter) {
        if (currentBook == book && currentChapter == chapter) {
            return;
        }

        currentBook = book;
        currentChapter = chapter;

        bookListAdapter.setSelected(currentBook, currentChapter);
        bookListAdapter.notifyDataSetChanged();

        drawerLayout.closeDrawers();

        versePagerAdapter.setSelected(currentBook, currentChapter, 0);
        versePagerAdapter.notifyDataSetChanged();

        versePager.setCurrentItem(currentChapter, true);

        updateTitle();
    }

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

    @Override
    public void onVersesSelectionChanged(boolean hasSelected) {
        if (hasSelected) {
            if (actionMode != null) {
                return;
            }

            actionMode = startSupportActionMode(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                    actionMode.getMenuInflater().inflate(R.menu.menu_text_selection_context, menu);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                    switch (menuItem.getItemId()) {
                        case R.id.action_copy:
                            Analytics.trackUIEvent("copy");

                            if (clipboardManager == null) {
                                // noinspection deprecation
                                clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            }
                            clipboardManager.setText(buildText(versePagerAdapter.getSelectedVerses(versePager.getCurrentItem())));
                            Toast.makeText(BibleReadingActivity.this,
                                    R.string.toast_verses_copied, Toast.LENGTH_SHORT).show();
                            actionMode.finish();
                            return true;
                        case R.id.action_share:
                            Analytics.trackUIEvent("share");

                            startActivity(Intent.createChooser(new Intent().setAction(Intent.ACTION_SEND).setType("text/plain")
                                            .putExtra(Intent.EXTRA_TEXT,
                                                    buildText(versePagerAdapter.getSelectedVerses(versePager.getCurrentItem()))),
                                    getResources().getText(R.string.text_share_with)
                            ));
                            actionMode.finish();
                            return true;
                        default:
                            return false;
                    }
                }

                @Override
                public void onDestroyActionMode(ActionMode actionMode) {
                    if (actionMode != BibleReadingActivity.this.actionMode) {
                        return;
                    }
                    versePagerAdapter.deselectVerses();
                    BibleReadingActivity.this.actionMode = null;
                }
            });
        } else {
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // do nothing
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        // do nothing
    }

    @Override
    public void onPageSelected(int position) {
        if (currentChapter == position) {
            return;
        }
        currentChapter = position;

        if (actionMode != null) {
            actionMode.finish();
        }

        updateBookList();
        updateTitle();
    }
}
