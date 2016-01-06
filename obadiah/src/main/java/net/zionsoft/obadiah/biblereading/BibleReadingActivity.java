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

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.view.ActionMode;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.biblereading.chapterselection.ChapterListView;
import net.zionsoft.obadiah.biblereading.chapterselection.ChapterPresenter;
import net.zionsoft.obadiah.biblereading.toolbar.BibleReadingToolbar;
import net.zionsoft.obadiah.biblereading.toolbar.ToolbarPresenter;
import net.zionsoft.obadiah.biblereading.verse.VersePagerPresenter;
import net.zionsoft.obadiah.biblereading.verse.VersePresenter;
import net.zionsoft.obadiah.biblereading.verse.VerseSelectionListener;
import net.zionsoft.obadiah.biblereading.verse.VerseViewPager;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.translations.TranslationManagementActivity;
import net.zionsoft.obadiah.ui.utils.BaseAppCompatActivity;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;

public class BibleReadingActivity extends BaseAppCompatActivity implements BibleReadingView,
        VerseSelectionListener, ActionMode.Callback, NfcAdapter.CreateNdefMessageCallback {
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
    ToolbarPresenter toolbarPresenter;

    @Inject
    ChapterPresenter chapterPresenter;

    @Inject
    VersePagerPresenter versePagerPresenter;

    @Inject
    VersePresenter versePresenter;

    @Inject
    Settings settings;

    @Bind(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @Bind(R.id.toolbar)
    BibleReadingToolbar toolbar;

    @Bind(R.id.chapter_list)
    ChapterListView chapterList;

    @Bind(R.id.verse_pager)
    VerseViewPager versePager;

    private AppIndexingManager appIndexingManager;

    private String currentTranslation;
    private List<String> bookNames;
    private int currentBook;
    private int currentChapter;
    private int currentVerse;

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
        updatePresenters();
    }

    private void initializeUi() {
        setContentView(R.layout.activity_bible_reading);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0);
        drawerLayout.setDrawerListener(drawerToggle);
    }

    private void updatePresenters() {
        // if the activity is recreated due to screen orientation change, the component fragment
        // is attached before the UI is initialized, i.e. onAttachFragment() is called inside
        // super.onCreate()
        // therefore, we try to do the initialization in both places

        if (toolbar != null && toolbarPresenter != null) {
            toolbar.setPresenter(toolbarPresenter);
        }

        if (chapterList != null && chapterPresenter != null) {
            chapterList.setPresenter(chapterPresenter);
        }

        if (settings != null && versePager != null && versePagerPresenter != null && versePresenter != null) {
            versePager.initialize(this, settings, this, versePagerPresenter, versePresenter);
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);

        if (fragment instanceof BibleReadingComponentFragment) {
            ((BibleReadingComponentFragment) fragment).getComponent().inject(this);
            updatePresenters();

            final View rootView = getWindow().getDecorView();
            rootView.setKeepScreenOn(settings.keepScreenOn());
            rootView.setBackgroundColor(settings.getBackgroundColor());

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

        currentTranslation = bibleReadingPresenter.loadCurrentTranslation();
        currentBook = bibleReadingPresenter.loadCurrentBook();
        currentChapter = bibleReadingPresenter.loadCurrentChapter();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        bibleReadingPresenter.takeView(this);
        toolbar.onResume();
        chapterList.onResume();
        versePager.onResume();

        if (TextUtils.isEmpty(bibleReadingPresenter.loadCurrentTranslation())) {
            DialogHelper.showDialog(this, false, R.string.error_no_translation,
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
            return;
        }

        loadBookNames();
    }

    private void loadBookNames() {
        bibleReadingPresenter.loadBookNamesForCurrentTranslation();
    }

    @Override
    protected void onPause() {
        bibleReadingPresenter.dropView();
        toolbar.onPause();
        chapterList.onPause();
        versePager.onPause();

        super.onPause();
    }

    @Override
    protected void onStop() {
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
    public void onBookNamesLoaded(List<String> bookNames) {
        this.bookNames = bookNames;
        trackingReadingProgress();
    }

    private void trackingReadingProgress() {
        if (bookNames == null) {
            // book names haven't been loaded yet, do nothing
            return;
        }

        appIndexingManager.onView(currentTranslation, bookNames.get(currentBook), currentBook, currentChapter);

        // TODO get an improved tracking algorithm, e.g. only consider as "read" if the user stays for a while
        bibleReadingPresenter.trackReadingProgress(currentBook, currentChapter);
    }

    @Override
    public void onBookNamesLoadFailed() {
        DialogHelper.showDialog(this, false, R.string.error_failed_to_load,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadBookNames();
                    }
                }, null);
    }

    @Override
    public void onTranslationUpdated(String translation) {
        currentTranslation = translation;
    }

    @Override
    public void onReadingProgressUpdated(Verse.Index index) {
        if (currentBook == index.book && currentChapter == index.chapter) {
            return;
        }
        currentBook = index.book;
        currentChapter = index.chapter;
        currentVerse = index.verse;

        trackingReadingProgress();

        drawerLayout.closeDrawer(GravityCompat.START);

        if (actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    public void onVersesSelectionChanged(boolean hasSelected) {
        if (hasSelected) {
            if (actionMode != null) {
                return;
            }
            actionMode = startSupportActionMode(this);
        } else {
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.menu_text_selection_context, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_copy:
                Analytics.trackEvent(Analytics.CATEGORY_UI, Analytics.UI_ACTION_BUTTON_CLICK, "copy");

                // noinspection deprecation
                final ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboardManager.setText(buildText(versePager.getSelectedVerses()));
                Toast.makeText(this, R.string.toast_verses_copied, Toast.LENGTH_SHORT).show();

                mode.finish();
                return true;
            case R.id.action_share:
                Analytics.trackEvent(Analytics.CATEGORY_UI, Analytics.UI_ACTION_BUTTON_CLICK, "share");

                // Facebook doesn't want us to pre-fill the message, but still captures ACTION_SEND
                // therefore, I have to exclude their package from being shown
                // it's a horrible way to force developers to use their SDK
                // ref. https://developers.facebook.com/bugs/332619626816423
                final Intent chooseIntent = createChooserExcludingPackage(
                        this, "com.facebook.katana", buildText(versePager.getSelectedVerses()));
                if (chooseIntent == null) {
                    Toast.makeText(this, R.string.error_unknown_error, Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(chooseIntent);
                }

                mode.finish();
                return true;
            default:
                return false;
        }
    }

    private static String buildText(@Nullable List<Verse> verses) {
        if (verses == null || verses.size() == 0) {
            return null;
        }

        // format: <book name> <chapter index>:<verse index> <verse text>
        final StringBuilder text = new StringBuilder();
        for (Verse verse : verses) {
            text.append(String.format("%S %d:%d %s\n", verse.bookName, verse.index.chapter + 1,
                    verse.index.verse + 1, verse.verseText));
        }
        return text.toString();
    }

    @Nullable
    private static Intent createChooserExcludingPackage(
            Context context, String packageToExclude, String text) {
        final Intent sendIntent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain");
        final PackageManager pm = context.getPackageManager();
        final List<ResolveInfo> resolveInfoList = pm.queryIntentActivities(sendIntent, 0);
        final int size = resolveInfoList.size();
        if (size == 0) {
            return null;
        }
        final ArrayList<Intent> filteredIntents = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            final ResolveInfo resolveInfo = resolveInfoList.get(i);
            final String packageName = resolveInfo.activityInfo.packageName;
            if (!packageToExclude.equals(packageName)) {
                final LabeledIntent labeledIntent = new LabeledIntent(
                        packageName, resolveInfo.loadLabel(pm), resolveInfo.getIconResource());
                labeledIntent.setAction(Intent.ACTION_SEND).setPackage(packageName)
                        .setComponent(new ComponentName(packageName, resolveInfo.activityInfo.name))
                        .setType("text/plain").putExtra(Intent.EXTRA_TEXT, text);
                filteredIntents.add(labeledIntent);
            }
        }

        final Intent chooserIntent = Intent.createChooser(filteredIntents.remove(0),
                context.getText(R.string.text_share_with));
        final int extraIntents = filteredIntents.size();
        if (extraIntents > 0) {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                    filteredIntents.toArray(new Parcelable[extraIntents]));
        }
        return chooserIntent;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        versePager.deselectVerses();
        this.actionMode = null;
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        return NfcHelper.createNdefMessage(this, currentTranslation, currentBook, currentChapter, currentVerse);
    }
}
