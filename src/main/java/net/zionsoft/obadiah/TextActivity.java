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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import net.zionsoft.obadiah.bible.TranslationReader;
import net.zionsoft.obadiah.util.SettingsManager;

import java.util.Iterator;
import java.util.LinkedList;

public class TextActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text_activity);

        mSettingsManager = new SettingsManager(this);
        mTranslationReader = new TranslationReader(this);

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

        final SharedPreferences.Editor editor = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE).edit();
        editor.putInt(Constants.PREF_KEY_LAST_READ_CHAPTER, mCurrentChapter);
        editor.putInt(Constants.PREF_KEY_LAST_READ_VERSE, mVersePagerAdapter.currentVerse());
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
                startActivity(new Intent(this, SearchActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
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

    private void initializeUi() {
        mRootView = getWindow().getDecorView();

        // initializes verses view pager
        mVerseViewPager = (ViewPager) findViewById(R.id.verse_viewpager);
        mVersePagerAdapter = new VersePagerAdapter();
        mVerseViewPager.setAdapter(mVersePagerAdapter);
        mVerseViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                mCurrentChapter = position;
                if (mActionMode != null)
                    mActionMode.finish();
                updateUi();
            }
        });
        mVerseViewPager.setPageTransformer(true, new DepthPageTransformer());
    }

    private void populateUi() {
        mRootView.setBackgroundColor(mSettingsManager.backgroundColor());

        final SharedPreferences preferences = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE);
        mCurrentBook = preferences.getInt(Constants.PREF_KEY_LAST_READ_BOOK, 0);
        mCurrentChapter = preferences.getInt(Constants.PREF_KEY_LAST_READ_CHAPTER, 0);
        mTranslationReader.selectTranslation(preferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null));

        mVersePagerAdapter.setSelection(preferences.getInt(Constants.PREF_KEY_LAST_READ_VERSE, 0));
        mVersePagerAdapter.updateText();
        mVerseViewPager.setCurrentItem(mCurrentChapter);

        updateUi();
    }

    private void updateUi() {
        setTitle(String.format("%s - %s, %d", mTranslationReader.selectedTranslationShortName(),
                mTranslationReader.bookNames()[mCurrentBook], (mCurrentChapter + 1)));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static class DepthPageTransformer implements ViewPager.PageTransformer {
        @Override
        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
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

                float scaleFactor = 0.7f + 0.3f * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            } else {
                view.setAlpha(0);
            }
        }
    }

    private class VersePagerAdapter extends PagerAdapter {
        public VersePagerAdapter() {
            super();
            mPages = new LinkedList<Page>();
        }

        public int getCount() {
            return (TextActivity.this.mCurrentBook < 0) ? 0 : TranslationReader
                    .chapterCount(TextActivity.this.mCurrentBook);
        }

        public Object instantiateItem(ViewGroup container, int position) {
            Iterator<Page> iterator = mPages.iterator();
            Page page = null;
            while (iterator.hasNext()) {
                page = iterator.next();
                if (page.inUse) {
                    page = null;
                    continue;
                }
                break;
            }

            if (page == null) {
                page = new Page();
                mPages.add(page);

                final ListView verseListView = new ListView(TextActivity.this);
                page.verseListView = verseListView;
                verseListView.setDivider(null);
                verseListView.setSelector(new ColorDrawable(Color.TRANSPARENT));

                final VerseListAdapter verseListAdapter = new VerseListAdapter(TextActivity.this,
                        mSettingsManager, mTranslationReader);
                page.verseListAdapter = verseListAdapter;
                verseListView.setAdapter(verseListAdapter);
                verseListView.setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        verseListAdapter.selectVerse(position);
                        if (verseListAdapter.hasVerseSelected()) {
                            if (mActionMode == null) {
                                mActionMode = TextActivity.this.startSupportActionMode(new ActionMode.Callback() {
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
                                                    mClipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                                }
                                                mClipboardManager.setText(mVersePagerAdapter.selectedText());
                                                Toast.makeText(TextActivity.this, R.string.toast_verses_copied, Toast.LENGTH_SHORT).show();
                                                actionMode.finish();
                                                return true;
                                            case R.id.action_share:
                                                final Intent intent = new Intent();
                                                intent.setAction(Intent.ACTION_SEND);
                                                intent.setType("text/plain");
                                                intent.putExtra(Intent.EXTRA_TEXT, mVersePagerAdapter.selectedText());
                                                startActivity(Intent.createChooser(intent, getResources().getText(R.string.title_share_with)));
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
                                        verseListAdapter.deselectVerses();
                                        mActionMode = null;
                                    }
                                });
                            }
                        } else {
                            if (mActionMode != null)
                                mActionMode.finish();
                        }
                    }
                });
            }

            container.addView(page.verseListView, 0);
            page.inUse = true;
            page.position = position;
            page.verseListAdapter.setCurrentChapter(mCurrentBook, position);

            // scroll to the correct position
            if (mSelection > 0 && position == TextActivity.this.mCurrentChapter) {
                page.verseListView.setSelection(mSelection);
                mSelection = 0;
            } else {
                page.verseListView.setSelectionAfterHeaderView();
            }

            return page;
        }

        public void destroyItem(ViewGroup container, int position, Object object) {
            for (Page page : mPages) {
                if (page.position == position) {
                    page.inUse = false;
                    container.removeView(page.verseListView);
                    return;
                }
            }
        }

        public boolean isViewFromObject(View view, Object object) {
            return view == ((Page) object).verseListView;
        }

        public void updateText() {
            for (Page page : mPages) {
                if (page.inUse)
                    page.verseListAdapter.setCurrentChapter(mCurrentBook, page.position);
            }

            notifyDataSetChanged();
        }

        public void setSelection(int selection) {
            mSelection = selection;
        }

        public int currentVerse() {
            for (Page page : mPages) {
                if (page.position == TextActivity.this.mCurrentChapter)
                    return page.verseListView.getFirstVisiblePosition();
            }
            return 0;
        }

        public String selectedText() {
            for (Page page : mPages) {
                if (page.position == TextActivity.this.mCurrentChapter)
                    return page.verseListAdapter.selectedText();
            }
            return null;
        }

        private class Page {
            public boolean inUse;
            public int position;
            public ListView verseListView;
            public VerseListAdapter verseListAdapter;
        }

        private int mSelection;
        private LinkedList<Page> mPages;
    }

    private int mCurrentBook = -1;
    private int mCurrentChapter = -1;
    private ActionMode mActionMode;
    private SettingsManager mSettingsManager;
    private TranslationReader mTranslationReader;
    private VersePagerAdapter mVersePagerAdapter;
    private ViewPager mVerseViewPager;
    private View mRootView;

    @SuppressWarnings("deprecation")
    private ClipboardManager mClipboardManager;
}
