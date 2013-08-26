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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import net.zionsoft.obadiah.util.SettingsManager;

public class TextActivity extends ActionBarActivity
        implements VersePagerAdapter.OnVerseSelectedListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text_activity);

        mSettingsManager = new SettingsManager(this);

        initializeUi();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSettingsManager.refresh();
        populateUi();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mActionMode != null)
            mActionMode.finish();

        final SharedPreferences.Editor editor
                = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE).edit();
        editor.putInt(Constants.PREF_KEY_LAST_READ_CHAPTER, mVersePagerAdapter.lastReadChapter());
        editor.putInt(Constants.PREF_KEY_LAST_READ_VERSE, mVersePagerAdapter.lastReadVerse());
        editor.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_text, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                startActivity(new Intent(this, SearchActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.action_select_translation:
                startActivity(new Intent(TextActivity.this, TranslationSelectionActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onVerseSelectionChanged(boolean hasVerseSelected) {
        if (hasVerseSelected) {
            if (mActionMode == null) {
                mActionMode = startSupportActionMode(new ActionMode.Callback() {
                    @Override
                    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                        actionMode.getMenuInflater().inflate(R.menu.menu_text_context, menu);
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
                                if (mClipboardManager == null) {
                                    // noinspection deprecation
                                    mClipboardManager = (ClipboardManager) getSystemService(
                                            Context.CLIPBOARD_SERVICE);
                                }
                                mClipboardManager.setText(mVersePagerAdapter.selectedText());
                                Toast.makeText(TextActivity.this, R.string.toast_verses_copied,
                                        Toast.LENGTH_SHORT).show();
                                actionMode.finish();
                                return true;
                            case R.id.action_share:
                                startActivity(Intent.createChooser(new Intent()
                                        .setAction(Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(Intent.EXTRA_TEXT,
                                                mVersePagerAdapter.selectedText()),
                                        getResources().getText(R.string.title_share_with)));
                                actionMode.finish();
                                return true;
                            default:
                                return false;
                        }
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode actionMode) {
                        if (actionMode != mActionMode)
                            return;
                        mVersePagerAdapter.deselectVerses();
                        mActionMode = null;
                    }
                });
            }
        } else {
            if (mActionMode != null)
                mActionMode.finish();
        }
    }

    private void initializeUi() {
        mRootView = getWindow().getDecorView();

        // initializes verses view pager
        mVerseViewPager = (ViewPager) findViewById(R.id.verse_viewpager);
        mVersePagerAdapter = new VersePagerAdapter(this, this, mSettingsManager);
        mVerseViewPager.setAdapter(mVersePagerAdapter);
        mVerseViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
                // do nothing
            }

            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
                // do nothing
            }

            public void onPageSelected(int position) {
                mVersePagerAdapter.setLastReadChapter(position);
                if (mActionMode != null)
                    mActionMode.finish();
                updateUi();
            }
        });
        mVerseViewPager.setPageTransformer(true, new DepthPageTransformer());
    }

    private void populateUi() {
        mRootView.setBackgroundColor(mSettingsManager.backgroundColor());

        // TODO caches verses for orientation change
        final SharedPreferences preferences
                = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        mVersePagerAdapter.setCurrentVerse(
                preferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null),
                preferences.getInt(Constants.PREF_KEY_LAST_READ_BOOK, 0),
                preferences.getInt(Constants.PREF_KEY_LAST_READ_CHAPTER, 0),
                preferences.getInt(Constants.PREF_KEY_LAST_READ_VERSE, 0));
        mVerseViewPager.setCurrentItem(mVersePagerAdapter.lastReadChapter());

        updateUi();
    }

    private void updateUi() {
        setTitle(String.format("%s, %d", mVersePagerAdapter.currentBookName(),
                mVersePagerAdapter.lastReadChapter() + 1));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static class DepthPageTransformer implements ViewPager.PageTransformer {
        @Override
        public void transformPage(View view, float position) {
            final int pageWidth = view.getWidth();
            if (position < -1) {
                view.setAlpha(0);
            } else if (position <= 0) {
                view.setAlpha(1);
                view.setTranslationX(0);
                view.setScaleX(1);
                view.setScaleY(1);
            } else if (position <= 1) {
                view.setAlpha(1 - position);
                view.setTranslationX(pageWidth * -position);

                final float scaleFactor = 0.7f + 0.3f * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            } else {
                view.setAlpha(0);
            }
        }
    }

    private ActionMode mActionMode;
    private SettingsManager mSettingsManager;
    private VersePagerAdapter mVersePagerAdapter;
    private ViewPager mVerseViewPager;
    private View mRootView;

    @SuppressWarnings("deprecation")
    private ClipboardManager mClipboardManager;
}
