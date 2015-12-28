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
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Bible;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

class VersePagerAdapter extends PagerAdapter implements VerseView {
    interface Listener {
        void onVersesSelectionChanged(boolean hasSelected);
    }

    static class Page implements RecyclerView.OnChildAttachStateChangeListener, View.OnClickListener {
        boolean inUse;
        int position;
        Listener listener;
        VerseListAdapter verseListAdapter;

        View rootView;

        @Bind(R.id.loading_spinner)
        View loadingSpinner;

        @Bind(R.id.verse_list)
        RecyclerView verseList;

        Page(View view) {
            ButterKnife.bind(this, view);
            rootView = view;
        }

        @Override
        public void onChildViewAttachedToWindow(View view) {
            view.setOnClickListener(this);
        }

        @Override
        public void onChildViewDetachedFromWindow(View view) {
            view.setOnClickListener(null);
        }

        @Override
        public void onClick(View v) {
            final int position = verseList.getChildAdapterPosition(v);
            verseListAdapter.select(position);
            verseListAdapter.notifyItemChanged(position);

            listener.onVersesSelectionChanged(verseListAdapter.hasSelectedVerses());
        }
    }

    private final Context context;
    private final VersePresenter versePresenter;
    private final Settings settings;
    private final Listener listener;
    private final LayoutInflater inflater;
    private final List<Page> pages;

    private String translationShortName;
    private int currentBook = -1;
    private int currentChapter;
    private int currentVerse;

    VersePagerAdapter(Context context, VersePresenter versePresenter, Settings settings,
                      Listener listener, int offScreenPageLimit) {
        super();

        this.context = context;
        this.versePresenter = versePresenter;
        this.settings = settings;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
        this.pages = new ArrayList<>(1 + 2 * offScreenPageLimit);
    }

    @Override
    public int getCount() {
        return currentBook < 0 || translationShortName == null ? 0 : Bible.getChapterCount(currentBook);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Page page = null;
        for (Page p : pages) {
            if (!p.inUse) {
                page = p;
                break;
            }
        }

        if (page == null) {
            page = new Page(inflater.inflate(R.layout.item_verse_pager, container, false));
            final VerseListAdapter verseListAdapter = new VerseListAdapter(context, settings);
            page.verseListAdapter = verseListAdapter;
            page.listener = listener;
            page.verseList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
            page.verseList.setAdapter(verseListAdapter);
            page.verseList.addOnChildAttachStateChangeListener(page);
            pages.add(page);
        }

        container.addView(page.rootView, 0);
        page.inUse = true;
        page.position = position;

        page.loadingSpinner.setVisibility(View.VISIBLE);
        page.verseList.setVisibility(View.GONE);
        loadVerses(position);

        return page;
    }

    private void loadVerses(int position) {
        versePresenter.loadVerses(translationShortName, currentBook, position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
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

    @Override
    public void onVersesLoaded(List<Verse> verses) {
        final int chapter = verses.get(0).chapterIndex;
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            if (page.position == chapter) {
                AnimationHelper.fadeOut(page.loadingSpinner);
                AnimationHelper.fadeIn(page.verseList);

                page.verseListAdapter.setVerses(verses);
                page.verseListAdapter.notifyDataSetChanged();

                if (currentVerse > 0 && currentChapter == chapter) {
                    page.verseList.post(new Runnable() {
                        @Override
                        public void run() {
                            page.verseList.scrollToPosition(currentVerse);
                            currentVerse = 0;
                        }
                    });
                } else {
                    page.verseList.scrollToPosition(0);
                }
                break;
            }
        }
    }

    @Override
    public void onVersesLoadFailed(String translation, int book, final int chapter) {
        DialogHelper.showDialog(context, false, R.string.dialog_retry,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadVerses(chapter);
                    }
                }, null);
    }

    void setTranslationShortName(String translationShortName) {
        this.translationShortName = translationShortName;
    }

    void setSelected(int currentBook, int currentChapter, int currentVerse) {
        this.currentBook = currentBook;
        this.currentChapter = currentChapter;
        this.currentVerse = currentVerse;
    }

    int getCurrentVerse(int chapter) {
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            if (page.position == chapter) {
                final LinearLayoutManager linearLayoutManager
                        = (LinearLayoutManager) page.verseList.getLayoutManager();
                return linearLayoutManager.findFirstVisibleItemPosition();
            }
        }
        return 0;
    }

    List<Verse> getSelectedVerses(int chapter) {
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            if (page.position == chapter)
                return page.verseListAdapter.getSelectedVerses();
        }
        return null;
    }

    void deselectVerses() {
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            page.verseListAdapter.deselectVerses();
            page.verseListAdapter.notifyDataSetChanged();
        }
    }
}
