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

package net.zionsoft.obadiah.biblereading.verse;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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
    static class Page implements RecyclerView.OnChildAttachStateChangeListener, View.OnClickListener {
        private final VerseSelectionListener listener;
        private final VerseListAdapter verseListAdapter;

        private boolean inUse;
        private int book;
        private int chapter;

        private final View rootView;

        @Bind(R.id.loading_spinner)
        View loadingSpinner;

        @Bind(R.id.verse_list)
        RecyclerView verseList;

        private Page(Context context, Settings settings, final VersePresenter versePresenter,
                     VerseSelectionListener listener, View rootView) {
            this.listener = listener;
            this.rootView = rootView;
            ButterKnife.bind(this, rootView);

            verseList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));

            verseListAdapter = new VerseListAdapter(context, settings);
            verseList.setAdapter(verseListAdapter);

            verseList.addOnChildAttachStateChangeListener(this);
            verseList.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);

                    if (inUse && newState == RecyclerView.SCROLL_STATE_IDLE) {
                        final LinearLayoutManager linearLayoutManager
                                = (LinearLayoutManager) recyclerView.getLayoutManager();
                        versePresenter.saveReadingProgress(book, chapter,
                                linearLayoutManager.findFirstVisibleItemPosition());
                    }
                }
            });
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
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            verseListAdapter.select(position);
            verseListAdapter.notifyItemChanged(position);

            listener.onVersesSelectionChanged(verseListAdapter.hasSelectedVerses());
        }
    }

    private final Context context;
    private final Settings settings;
    private final VersePresenter versePresenter;
    private final LayoutInflater inflater;
    private final ArrayList<Page> pages;

    private VerseSelectionListener listener;

    private String translation;
    private int currentBook;
    private int currentChapter;
    private int currentVerse;

    VersePagerAdapter(Context context, Settings settings,
                      VersePresenter versePresenter, int offScreenPageLimit) {
        this.context = context;
        this.settings = settings;
        this.versePresenter = versePresenter;
        this.inflater = LayoutInflater.from(context);
        this.pages = new ArrayList<>(1 + 2 * offScreenPageLimit);

        translation = versePresenter.loadCurrentTranslation();
        currentBook = versePresenter.loadCurrentBook();
        currentChapter = versePresenter.loadCurrentChapter();
        currentVerse = versePresenter.loadCurrentVerse();
    }

    @Override
    public int getCount() {
        return listener == null || TextUtils.isEmpty(translation) ? 0 : Bible.getChapterCount(currentBook);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Page page = null;
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page p = pages.get(i);
            if (!p.inUse) {
                page = p;
                break;
            }
        }
        if (page == null) {
            page = new Page(context, settings, versePresenter, listener,
                    inflater.inflate(R.layout.item_verse_pager, container, false));
            pages.add(page);
        }
        container.addView(page.rootView, 0);

        page.inUse = true;
        page.book = currentBook;
        page.chapter = position;

        page.loadingSpinner.setVisibility(View.VISIBLE);
        page.verseList.setVisibility(View.GONE);

        loadVerses(position);

        return page;
    }

    private void loadVerses(int chapter) {
        versePresenter.loadVerses(translation, currentBook, chapter);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            if (page.chapter == position) {
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
        final int chapter = verses.get(0).index.chapter;
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            if (page.chapter == chapter) {
                AnimationHelper.fadeOut(page.loadingSpinner);
                AnimationHelper.fadeIn(page.verseList);

                page.verseListAdapter.setVerses(verses);
                page.verseListAdapter.notifyDataSetChanged();

                if (currentVerse > 0 && currentChapter == chapter) {
                    page.verseList.post(new Runnable() {
                        @Override
                        public void run() {
                            ((LinearLayoutManager) page.verseList.getLayoutManager())
                                    .scrollToPositionWithOffset(currentVerse, 0);
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

    @Override
    public void onTranslationUpdated(String translation) {
        this.translation = translation;
        notifyDataSetChanged();
    }

    @Override
    public void onReadingProgressUpdated(Verse.Index index) {
        currentChapter = index.chapter;
        currentVerse = index.verse;
        if (currentBook == index.book) {
            return;
        }
        currentBook = index.book;
        notifyDataSetChanged();
    }

    void onResume() {
        versePresenter.takeView(this);

        boolean isReadingProgressChanged = false;
        final String translation = versePresenter.loadCurrentTranslation();
        if (!TextUtils.isEmpty(translation) && !translation.equals(this.translation)) {
            this.translation = translation;
            isReadingProgressChanged = true;
        }
        final int book = versePresenter.loadCurrentBook();
        if (book != currentBook) {
            currentBook = book;
            isReadingProgressChanged = true;
        }
        if (isReadingProgressChanged) {
            notifyDataSetChanged();
        }
    }

    void onPause() {
        versePresenter.dropView();
    }

    void setVerseSelectionListener(VerseSelectionListener listener) {
        this.listener = listener;
        notifyDataSetChanged();
    }

    @Nullable
    List<Verse> getSelectedVerses(int chapter) {
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            if (page.chapter == chapter)
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
