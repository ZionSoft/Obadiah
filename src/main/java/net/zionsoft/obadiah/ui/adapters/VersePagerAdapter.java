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

package net.zionsoft.obadiah.ui.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.Verse;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.LinkedList;
import java.util.List;

public class VersePagerAdapter extends PagerAdapter {
    public static interface Listener {
        public void onVersesSelectionChanged(boolean hasSelected);
    }

    private static class Page {
        boolean inUse;
        int position;
        VerseListAdapter verseListAdapter;

        View rootView;
        View loadingSpinner;
        ListView verseListView;
    }

    private final Context mContext;
    private final Listener mListener;
    private final Bible mBible;
    private final List<Page> mPages;

    private String mTranslationShortName;
    private int mCurrentBook = -1;
    private int mCurrentChapter;
    private int mCurrentVerse;

    public VersePagerAdapter(Context context, Listener listener) {
        super();

        mContext = context;
        mListener = listener;
        mBible = Bible.getInstance();
        mPages = new LinkedList<Page>();
    }

    @Override
    public int getCount() {
        return mCurrentBook < 0 || mTranslationShortName == null ? 0 : Bible.getChapterCount(mCurrentBook);
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
            page.rootView = View.inflate(mContext, R.layout.item_verse_pager, null);
            page.loadingSpinner = page.rootView.findViewById(R.id.loading_spinner);
            page.verseListView = (ListView) page.rootView.findViewById(R.id.verse_list_view);
            final VerseListAdapter verseListAdapter = new VerseListAdapter(mContext);
            page.verseListAdapter = verseListAdapter;
            page.verseListView.setAdapter(verseListAdapter);
            page.verseListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    verseListAdapter.select(position);
                    verseListAdapter.notifyDataSetChanged();

                    mListener.onVersesSelectionChanged(verseListAdapter.hasSelectedVerses());
                }
            });
            mPages.add(page);
        }

        container.addView(page.rootView, 0);
        page.inUse = true;
        page.position = position;

        page.loadingSpinner.setVisibility(View.VISIBLE);
        page.verseListView.setVisibility(View.GONE);
        loadVerses(position, page);

        return page;
    }

    private void loadVerses(final int position, final Page page) {
        mBible.loadVerses(mTranslationShortName, mCurrentBook, position, new Bible.OnVersesLoadedListener() {
                    @Override
                    public void onVersesLoaded(List<Verse> verses) {
                        if (verses == null || verses.size() == 0) {
                            DialogHelper.showDialog(mContext, false, R.string.dialog_retry,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            loadVerses(position, page);
                                        }
                                    }, null
                            );
                            return;
                        }

                        if (page.position == position) {
                            AnimationHelper.fadeOut(page.loadingSpinner);
                            AnimationHelper.fadeIn(page.verseListView);

                            page.verseListAdapter.setVerses(verses);
                            page.verseListAdapter.notifyDataSetChanged();

                            if (mCurrentVerse > 0 && mCurrentChapter == position) {
                                page.verseListView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        page.verseListView.setSelection(mCurrentVerse);
                                        mCurrentVerse = 0;
                                    }
                                });
                            } else {
                                page.verseListView.setSelectionAfterHeaderView();
                            }
                        }
                    }
                }
        );
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        for (Page page : mPages) {
            if (page.position == position) {
                page.inUse = false;
                container.removeView(page.rootView);
                return;
            }
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((Page) object).rootView;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    public void setTranslationShortName(String translationShortName) {
        mTranslationShortName = translationShortName;
    }

    public void setSelected(int currentBook, int currentChapter, int currentVerse) {
        mCurrentBook = currentBook;
        mCurrentChapter = currentChapter;
        mCurrentVerse = currentVerse;
    }

    public int getCurrentVerse(int chapter) {
        for (Page page : mPages) {
            if (page.position == chapter)
                return page.verseListView.getFirstVisiblePosition();
        }
        return 0;
    }

    public List<Verse> getSelectedVerses(int chapter) {
        for (Page page : mPages) {
            if (page.position == chapter)
                return page.verseListAdapter.getSelectedVerses();
        }
        return null;
    }

    public void deselectVerses() {
        for (Page page : mPages) {
            page.verseListAdapter.deselectVerses();
            page.verseListAdapter.notifyDataSetChanged();
        }
    }
}
