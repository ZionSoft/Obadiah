/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2016 ZionSoft
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

package net.zionsoft.obadiah.biblereading.verse;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseIndex;

import java.util.ArrayList;
import java.util.List;

public class VerseViewPager extends ViewPager implements VerseView, VerseSelectionListener,
        ActionMode.Callback {
    private final ViewPager.SimpleOnPageChangeListener onPageChangeListener
            = new ViewPager.SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            if (currentChapter == position) {
                return;
            }
            versePagerPresenter.saveReadingProgress(currentBook, position, 0);
        }
    };

    private AppCompatActivity activity;
    private ActionMode actionMode;

    VersePresenter versePagerPresenter;

    private VersePagerAdapter adapter;
    int currentBook;
    int currentChapter;

    public VerseViewPager(Context context) {
        super(context);
    }

    public VerseViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onReadingProgressUpdated(VerseIndex index) {
        boolean chapterChanged = false;
        if (currentChapter != index.chapter()) {
            currentChapter = index.chapter();
            chapterChanged = true;
        }

        boolean bookChanged = false;
        if (currentBook != index.book()) {
            currentBook = index.book();
            adapter.setReadingProgress(index);
            bookChanged = true;
        }

        // removes the listener here, otherwise the callback will be invoked and update reading
        // progress unexpectedly
        removeOnPageChangeListener(onPageChangeListener);
        if (bookChanged) {
            adapter.notifyDataSetChanged();
        }
        if (chapterChanged) {
            setCurrentItem(currentChapter, true);
        }
        addOnPageChangeListener(onPageChangeListener);

        if ((bookChanged || chapterChanged) && actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    public void onVersesSelectionChanged(boolean hasSelected) {
        if (hasSelected) {
            if (actionMode != null) {
                return;
            }
            actionMode = activity.startSupportActionMode(this);
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

                final List<Verse> verses = adapter.getSelectedVerses(getCurrentItem());
                if (verses != null && verses.size() > 0) {
                    final ClipboardManager clipboardManager
                            = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    final Verse verse = verses.get(0);
                    clipboardManager.setPrimaryClip(ClipData.newPlainText(
                            verse.text.translation + " " + verse.text.bookName, buildText(verses)));
                    Toast.makeText(activity, R.string.toast_verses_copied, Toast.LENGTH_SHORT).show();
                }

                mode.finish();
                return true;
            case R.id.action_share:
                Analytics.trackEvent(Analytics.CATEGORY_UI, Analytics.UI_ACTION_BUTTON_CLICK, "share");

                // Facebook doesn't want us to pre-fill the message, but still captures ACTION_SEND
                // therefore, I have to exclude their package from being shown
                // it's a horrible way to force developers to use their SDK
                // ref. https://developers.facebook.com/bugs/332619626816423
                final Intent chooseIntent = createChooserExcludingPackage(activity,
                        "com.facebook.katana", buildText(adapter.getSelectedVerses(getCurrentItem())));
                if (chooseIntent == null) {
                    Toast.makeText(activity, R.string.error_unknown_error, Toast.LENGTH_SHORT).show();
                } else {
                    activity.startActivity(chooseIntent);
                }

                mode.finish();
                return true;
            default:
                return false;
        }
    }

    @Nullable
    private static String buildText(@Nullable List<Verse> verses) {
        final int versesCount = verses != null ? verses.size() : 0;
        if (versesCount == 0) {
            return null;
        }

        // TODO supports selection for verses with parallel translations
        // format: <book name> <chapter verseIndex>:<verse verseIndex> <verse text>
        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < versesCount; ++i) {
            final Verse verse = verses.get(i);
            text.append(verse.text.bookName).append(' ').append(verse.verseIndex.chapter() + 1).append(':')
                    .append(verse.verseIndex.verse() + 1).append(' ').append(verse.text.text).append('\n');
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
        adapter.deselectVerses();
        this.actionMode = null;
    }

    public void initialize(AppCompatActivity activity, VersePresenter versePagerPresenter,
                           VersePagerPresenter versePresenter) {
        this.activity = activity;
        this.versePagerPresenter = versePagerPresenter;

        adapter = new VersePagerAdapter(activity, versePresenter, getOffscreenPageLimit());
        adapter.setVerseSelectionListener(this);
        setAdapter(adapter);
    }

    public void onStart() {
        versePagerPresenter.takeView(this);
        currentBook = versePagerPresenter.loadCurrentBook();
        currentChapter = versePagerPresenter.loadCurrentChapter();

        adapter.onStart();
        setCurrentItem(currentChapter);

        // should add the listener after setting current item and current book / chapter is loaded,
        // otherwise, the callback will be triggered with wrong book / chapter
        addOnPageChangeListener(onPageChangeListener);
    }

    public void onStop() {
        if (actionMode != null) {
            actionMode.finish();
        }

        removeOnPageChangeListener(onPageChangeListener);
        versePagerPresenter.dropView();
        adapter.onStop();
    }
}
