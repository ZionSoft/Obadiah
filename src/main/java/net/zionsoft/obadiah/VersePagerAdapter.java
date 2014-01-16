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

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import net.zionsoft.obadiah.bible.TranslationReader;
import net.zionsoft.obadiah.util.SettingsManager;

import java.util.LinkedList;
import java.util.List;

class VersePagerAdapter extends PagerAdapter {
    static interface OnVerseSelectedListener {
        public void onVerseSelectionChanged(boolean hasVerseSelected);
    }

    VersePagerAdapter(Context context, OnVerseSelectedListener onVerseSelectedListener,
                      SettingsManager settingsManager) {
        super();
        mContext = context;
        mOnVerseSelectedListener = onVerseSelectedListener;
        mSettingsManager = settingsManager;
        mTranslationReader = new TranslationReader(mContext);
        mPages = new LinkedList<Page>();
    }

    @Override
    public int getCount() {
        try {
            return (mCurrentBook < 0) ? 0 : TranslationReader.chapterCount(mCurrentBook);
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Page page = null;
        for (Page p : mPages) {
            if (!p.inUse) {
                page = p;
                break;
            }
        }

        if (page == null) {
            page = new Page();
            page.verseListView = (ListView) View.inflate(mContext, R.layout.text_verse_page, null);
            final VerseListAdapter verseListAdapter
                    = new VerseListAdapter(mContext, mSettingsManager);
            page.verseListAdapter = verseListAdapter;
            page.verseListView.setAdapter(verseListAdapter);
            page.verseListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    verseListAdapter.selectVerse(position);
                    mOnVerseSelectedListener.onVerseSelectionChanged(
                            verseListAdapter.hasVerseSelected());
                }
            });
            mPages.add(page);
        }

        container.addView(page.verseListView, 0);
        page.inUse = true;
        page.position = position;

        // TODO no data base operation in main thread
        page.verseListAdapter.setVerses(mCurrentBookName, position,
                mTranslationReader.verses(mCurrentBook, position));

        if (mLastReadVerse > 0 && mLastReadChapter == position) {
            page.verseListView.setSelection(mLastReadVerse);
            mLastReadVerse = 0;
        } else {
            page.verseListView.setSelectionAfterHeaderView();
        }

        return page;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        for (Page page : mPages) {
            if (page.position == position) {
                page.inUse = false;
                container.removeView(page.verseListView);
                return;
            }
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((Page) object).verseListView;
    }

    void setCurrentVerse(String translationShortName, int book, int chapter, int verse) {
        // TODO no data base operation in main thread
        mTranslationReader.selectTranslation(translationShortName);
        mCurrentBookName = mTranslationReader.bookNames()[book];

        mCurrentBook = book;
        mLastReadChapter = chapter;
        mLastReadVerse = verse;

        for (Page page : mPages) {
            if (page.inUse) {
                // TODO no data base operation in main thread
                page.verseListAdapter.setVerses(mCurrentBookName, page.position,
                        mTranslationReader.verses(mCurrentBook, page.position));
            }
        }

        notifyDataSetChanged();
    }

    void setLastReadChapter(int lastReadChapter) {
        mLastReadChapter = lastReadChapter;
    }

    String currentBookName() {
        return mCurrentBookName;
    }

    int lastReadChapter() {
        return mLastReadChapter;
    }

    int lastReadVerse() {
        for (Page page : mPages) {
            if (page.position == mLastReadChapter) {
                mLastReadVerse = page.verseListView.getFirstVisiblePosition();
                return mLastReadVerse;
            }
        }
        return 0;
    }


    // verses selection

    String selectedText() {
        for (Page page : mPages) {
            if (page.position == mLastReadChapter)
                return page.verseListAdapter.selectedText();
        }
        return null;
    }

    void deselectVerses() {
        for (Page page : mPages) {
            if (page.inUse)
                page.verseListAdapter.deselectVerses();
        }
    }

    private static class Page {
        public boolean inUse;
        public int position;
        public ListView verseListView;
        public VerseListAdapter verseListAdapter;
    }

    private final Context mContext;
    private final OnVerseSelectedListener mOnVerseSelectedListener;
    private final SettingsManager mSettingsManager;
    private final TranslationReader mTranslationReader;
    private final List<Page> mPages;

    private String mCurrentBookName;
    private int mCurrentBook = -1;
    private int mLastReadChapter;
    private int mLastReadVerse;
}
