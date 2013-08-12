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
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
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
                populateUi();
            }
        });
        mVerseViewPager.setPageTransformer(true, new DepthPageTransformer());
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSettingsManager.refresh();
        mBackgroundColor = mSettingsManager.backgroundColor();
        mTextColor = mSettingsManager.textColor();

        final SharedPreferences preferences = getSharedPreferences(Constants.SETTING_KEY, MODE_PRIVATE);
        mCurrentBook = preferences.getInt(Constants.CURRENT_BOOK_SETTING_KEY, 0);
        mCurrentChapter = preferences.getInt(Constants.CURRENT_CHAPTER_SETTING_KEY, 0);
        mTranslationReader.selectTranslation(preferences.getString(Constants.CURRENT_TRANSLATION_SETTING_KEY, null));

        mVersePagerAdapter.setSelection(preferences.getInt(Constants.CURRENT_VERSE_SETTING_KEY, 0));
        mVersePagerAdapter.updateText();
        mVerseViewPager.setCurrentItem(mCurrentChapter);

        populateUi();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mActionMode != null)
            mActionMode.finish();

        final SharedPreferences.Editor editor = getSharedPreferences(Constants.SETTING_KEY, MODE_PRIVATE).edit();
        editor.putInt(Constants.CURRENT_CHAPTER_SETTING_KEY, mCurrentChapter);
        editor.putInt(Constants.CURRENT_VERSE_SETTING_KEY, mVersePagerAdapter.currentVerse());
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

    private void populateUi() {
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

    private class VerseListAdapter extends ListBaseAdapter {
        public VerseListAdapter(Context context) {
            super(context);
        }

        public void selectItem(int position) {
            if (position < 0 || position >= mTexts.length)
                return;

            mSelected[position] ^= true;
            if (mSelected[position])
                ++mSelectedCount;
            else
                --mSelectedCount;

            notifyDataSetChanged();
        }

        public boolean hasItemSelected() {
            return (mSelectedCount > 0);
        }

        public void deselect() {
            int length = mSelected.length;
            for (int i = 0; i < length; ++i)
                mSelected[i] = false;
            mSelectedCount = 0;

            notifyDataSetChanged();
        }

        public String selectedText() {
            if (!hasItemSelected())
                return null;

            // format: <book name> <chapter index>:<verse index> <verse text>
            final String template = String.format("%s %d:%d %s",
                    TextActivity.this.mTranslationReader.bookNames()[TextActivity.this.mCurrentBook],
                    TextActivity.this.mCurrentChapter + 1);
            StringBuilder selected = new StringBuilder();
            for (int i = 0; i < mTexts.length; ++i) {
                if (mSelected[i]) {
                    if (selected.length() != 0)
                        selected.append("\n");
                    selected.append(String.format(template, i + 1, mTexts[i]));
                }
            }
            return selected.toString();
        }

        public void setTexts(String[] texts) {
            mTexts = texts;

            final int length = texts.length;
            if (mSelected == null || length > mSelected.length)
                mSelected = new boolean[length];
            for (int i = 0; i < length; ++i)
                mSelected[i] = false;
            mSelectedCount = 0;

            notifyDataSetChanged();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout linearLayout;
            if (convertView == null)
                linearLayout = (LinearLayout) View.inflate(mContext, R.layout.text_list_item, null);
            else
                linearLayout = (LinearLayout) convertView;

            TextView textView = (TextView) linearLayout.getChildAt(0);
            textView.setTextColor(TextActivity.this.mTextColor);
            textView.setText(Integer.toString(position + 1));

            textView = (TextView) linearLayout.getChildAt(1);
            textView.setTextColor(TextActivity.this.mTextColor);
            if (mSelected[position]) {
                final SpannableString string = new SpannableString(mTexts[position]);
                if (mBackgroundColorSpan == null)
                    mBackgroundColorSpan = new BackgroundColorSpan(Color.LTGRAY);
                string.setSpan(mBackgroundColorSpan, 0, mTexts[position].length(),
                        SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
                textView.setText(string);
            } else {
                textView.setText(mTexts[position]);
            }

            return linearLayout;
        }

        private boolean mSelected[];
        private int mSelectedCount;
        private BackgroundColorSpan mBackgroundColorSpan;
    }

    private class VersePagerAdapter extends PagerAdapter {
        public VersePagerAdapter() {
            super();
            m_pages = new LinkedList<Page>();
        }

        public int getCount() {
            return (TextActivity.this.mCurrentBook < 0) ? 0 : TranslationReader
                    .chapterCount(TextActivity.this.mCurrentBook);
        }

        public Object instantiateItem(ViewGroup container, int position) {
            Iterator<Page> iterator = m_pages.iterator();
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
                m_pages.add(page);

                final ListView verseListView = new ListView(TextActivity.this);
                page.verseListView = verseListView;
                verseListView.setBackgroundColor(TextActivity.this.mBackgroundColor);
                verseListView.setCacheColorHint(TextActivity.this.mBackgroundColor);
                verseListView.setDivider(null);
                verseListView.setSelector(new ColorDrawable(Color.TRANSPARENT));

                final VerseListAdapter verseListAdapter = new VerseListAdapter(TextActivity.this);
                page.verseListAdapter = verseListAdapter;
                verseListView.setAdapter(verseListAdapter);
                verseListView.setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        verseListAdapter.selectItem(position);
                        if (verseListAdapter.hasItemSelected()) {
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
                                                Toast.makeText(TextActivity.this, R.string.text_copied, Toast.LENGTH_SHORT).show();
                                                actionMode.finish();
                                                return true;
                                            case R.id.action_share:
                                                final Intent intent = new Intent();
                                                intent.setAction(Intent.ACTION_SEND);
                                                intent.setType("text/plain");
                                                intent.putExtra(Intent.EXTRA_TEXT, mVersePagerAdapter.selectedText());
                                                startActivity(Intent.createChooser(intent, getResources().getText(R.string.text_share_with)));
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
                                        verseListAdapter.deselect();
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
            page.verseListAdapter.setTexts(TextActivity.this.mTranslationReader.verses(
                    TextActivity.this.mCurrentBook, position));

            // scroll to the correct position
            if (m_selection > 0 && position == TextActivity.this.mCurrentChapter) {
                page.verseListView.setSelection(m_selection);
                m_selection = 0;
            } else {
                page.verseListView.setSelectionAfterHeaderView();
            }

            return page;
        }

        public void destroyItem(ViewGroup container, int position, Object object) {
            for (Page page : m_pages) {
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
            for (Page page : m_pages) {
                page.verseListView.setBackgroundColor(TextActivity.this.mBackgroundColor);
                page.verseListView.setCacheColorHint(TextActivity.this.mBackgroundColor);
                if (page.inUse) {
                    page.verseListAdapter.setTexts(TextActivity.this.mTranslationReader.verses(
                            TextActivity.this.mCurrentBook, page.position));
                }
            }

            notifyDataSetChanged();
        }

        public void setSelection(int selection) {
            m_selection = selection;
        }

        public int currentVerse() {
            for (Page page : m_pages) {
                if (page.position == TextActivity.this.mCurrentChapter)
                    return page.verseListView.getFirstVisiblePosition();
            }
            return 0;
        }

        public String selectedText() {
            for (Page page : m_pages) {
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

        private int m_selection;
        private LinkedList<Page> m_pages;
    }

    private int mCurrentBook = -1;
    private int mCurrentChapter = -1;
    private int mBackgroundColor;
    private int mTextColor;
    private ActionMode mActionMode;
    private SettingsManager mSettingsManager;
    private TranslationReader mTranslationReader;
    private VersePagerAdapter mVersePagerAdapter;
    private ViewPager mVerseViewPager;

    @SuppressWarnings("deprecation")
    private ClipboardManager mClipboardManager;
}
