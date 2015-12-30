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
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import android.widget.Toast;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.translations.TranslationManagementActivity;
import net.zionsoft.obadiah.ui.utils.BaseAppCompatActivity;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;

public class BibleReadingActivity extends BaseAppCompatActivity implements BibleReadingView,
        VersePagerAdapter.Listener, ViewPager.OnPageChangeListener,
        NfcAdapter.CreateNdefMessageCallback {
    private static final String KEY_MESSAGE_TYPE = "net.zionsoft.obadiah.KEY_MESSAGE_TYPE";
    private static final String KEY_BOOK_INDEX = "net.zionsoft.obadiah.KEY_BOOK_INDEX";
    private static final String KEY_CHAPTER_INDEX = "net.zionsoft.obadiah.KEY_CHAPTER_INDEX";
    private static final String KEY_VERSE_INDEX = "net.zionsoft.obadiah.KEY_VERSE_INDEX";

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

    @Inject
    BibleReadingPresenter bibleReadingPresenter;

    @Inject
    VersePresenter versePresenter;

    @Inject
    Settings settings;

    @Bind(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.verse_pager)
    ViewPager versePager;

    private AppIndexingManager appIndexingManager;

    private String currentTranslation;
    private List<String> bookNames;
    private int currentBook;
    private int currentChapter;

    private VersePagerAdapter versePagerAdapter;
    private ActionMode actionMode;

    private ActionBarDrawerToggle drawerToggle;

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

        NfcHelper.registerNdefMessageCallback(this, this);
        appIndexingManager = new AppIndexingManager(this);

        initializeUi();
    }

    private void initializeUi() {
        setContentView(R.layout.activity_bible_reading);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0);
        drawerLayout.setDrawerListener(drawerToggle);

        initializeAdapter();
    }

    private void initializeAdapter() {
        if (versePager == null || versePresenter == null || settings == null || versePagerAdapter != null) {
            // if the activity is recreated due to screen orientation change, the component fragment
            // is attached before the UI is initialized, i.e. onAttachFragment() is called inside
            // super.onCreate()
            // therefore, we try to do the initialization in both places
            return;
        }
        versePagerAdapter = new VersePagerAdapter(this, versePresenter, settings, this, versePager.getOffscreenPageLimit());
        versePager.setAdapter(versePagerAdapter);
        versePager.addOnPageChangeListener(this);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        if (fragment instanceof BibleReadingComponentFragment) {
            ((BibleReadingComponentFragment) fragment).getComponent().inject(this);

            final View rootView = getWindow().getDecorView();
            rootView.setKeepScreenOn(settings.keepScreenOn());
            rootView.setBackgroundColor(settings.getBackgroundColor());

            initializeAdapter();
            checkDeepLink();
        }
    }

    private void checkDeepLink() {
        final Intent startIntent = getIntent();
        final Uri uri = startIntent.getData();
        if (uri != null) {
            UriHelper.checkUri(bibleReadingPresenter, uri);
            startIntent.setData(null);
        } else {
            final String messageType = startIntent.getStringExtra(KEY_MESSAGE_TYPE);
            if (TextUtils.isEmpty(messageType)) {
                return;
            }
            final int bookIndex = startIntent.getIntExtra(KEY_BOOK_INDEX, -1);
            final int chapterIndex = startIntent.getIntExtra(KEY_CHAPTER_INDEX, -1);
            final int verseIndex = startIntent.getIntExtra(KEY_VERSE_INDEX, -1);
            if (bookIndex < 0 || chapterIndex < 0 || verseIndex < 0) {
                // should not happen, but just in case
                return;
            }
            startIntent.putExtra(KEY_MESSAGE_TYPE, (String) null)
                    .putExtra(KEY_BOOK_INDEX, -1)
                    .putExtra(KEY_CHAPTER_INDEX, -1)
                    .putExtra(KEY_VERSE_INDEX, -1);

            bibleReadingPresenter.saveReadingProgress(bookIndex, chapterIndex, verseIndex);

            Analytics.trackEvent(Analytics.CATEGORY_NOTIFICATION, Analytics.NOTIFICATION_ACTION_OPENED, messageType);
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
        final View rootView = getWindow().getDecorView();
        rootView.setKeepScreenOn(settings.keepScreenOn());
        rootView.setBackgroundColor(settings.getBackgroundColor());

        versePagerAdapter.notifyDataSetChanged();

        currentTranslation = bibleReadingPresenter.loadCurrentTranslation();
        currentBook = bibleReadingPresenter.loadCurrentBook();
        currentChapter = bibleReadingPresenter.loadCurrentChapter();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        bibleReadingPresenter.takeView(this);
        versePresenter.takeView(versePagerAdapter);

        loadTranslations();
        loadBookNames();
    }

    private void loadTranslations() {
        bibleReadingPresenter.loadTranslations();
    }

    private void loadBookNames() {
        bibleReadingPresenter.loadBookNamesForCurrentTranslation();
    }

    @Override
    protected void onPause() {
        bibleReadingPresenter.dropView();
        versePresenter.dropView();
        super.onPause();
    }

    @Override
    protected void onStop() {
        bibleReadingPresenter.saveReadingProgress(currentBook, currentChapter,
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
    public void onTranslationsLoaded(List<String> translations) {
        loadVerses();
    }

    private void loadVerses() {
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
                        loadTranslations();
                    }
                }, null);
    }

    @Override
    public void onNoTranslationAvailable() {
        DialogHelper.showDialog(this, false, R.string.dialog_no_translation,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(TranslationManagementActivity.newStartIntent(BibleReadingActivity.this));
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
    }

    private void updateTitle() {
        final String bookName = bookNames.get(currentBook);
        appIndexingManager.onView(currentTranslation, bookName, currentBook, currentChapter);

        // TODO get an improved tracking algorithm, e.g. only consider as "read" if the user stays for a while
        bibleReadingPresenter.trackReadingProgress(currentBook, currentChapter);
    }

    @Override
    public void onBookNamesLoadFailed() {
        DialogHelper.showDialog(this, false, R.string.dialog_retry,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadBookNames();
                    }
                }, null);
    }

    // TODO
//    @Override
//    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//        bibleReadingPresenter.saveReadingProgress(currentBook, currentChapter,
//                versePagerAdapter.getCurrentVerse(versePager.getCurrentItem()));
//
//        loadVerses();
//    }

//    @Override
//    public void onChapterSelected(int book, int chapter) {
//        if (currentBook == book && currentChapter == chapter) {
//            return;
//        }
//
//        drawerLayout.closeDrawers();
//
//        versePagerAdapter.setSelected(currentBook, currentChapter, 0);
//        versePagerAdapter.notifyDataSetChanged();
//
//        versePager.setCurrentItem(currentChapter, true);
//
//        updateTitle();
//    }

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
                            Analytics.trackEvent(Analytics.CATEGORY_UI, Analytics.UI_ACTION_BUTTON_CLICK, "copy");

                            // noinspection deprecation
                            final ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboardManager.setText(buildText(versePagerAdapter.getSelectedVerses(versePager.getCurrentItem())));
                            Toast.makeText(BibleReadingActivity.this,
                                    R.string.toast_verses_copied, Toast.LENGTH_SHORT).show();
                            actionMode.finish();
                            return true;
                        case R.id.action_share:
                            Analytics.trackEvent(Analytics.CATEGORY_UI, Analytics.UI_ACTION_BUTTON_CLICK, "share");

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

    private static String buildText(List<Verse> verses) {
        if (verses == null || verses.size() == 0)
            return null;

        // format: <book name> <chapter index>:<verse index> <verse text>
        final StringBuilder text = new StringBuilder();
        for (Verse verse : verses) {
            text.append(String.format("%S %d:%d %s\n", verse.bookName, verse.index.chapter + 1,
                    verse.index.verse + 1, verse.verseText));
        }
        return text.toString();
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
        bibleReadingPresenter.saveReadingProgress(currentBook, currentChapter, 0);

        if (actionMode != null) {
            actionMode.finish();
        }

        updateTitle();
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        return NfcHelper.createNdefMessage(this, currentTranslation, currentBook, currentChapter,
                versePagerAdapter.getCurrentVerse(versePager.getCurrentItem()));
    }
}
