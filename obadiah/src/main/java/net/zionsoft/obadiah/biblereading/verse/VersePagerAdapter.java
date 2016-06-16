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
import net.zionsoft.obadiah.model.domain.Bible;
import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.Note;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

class VersePagerAdapter extends PagerAdapter implements VersePagerView {
    static class Page implements RecyclerView.OnChildAttachStateChangeListener, View.OnClickListener {
        private final VerseSelectionListener listener;
        private final VerseListAdapter verseListAdapter;

        private boolean inUse;
        private int book;
        private int chapter;

        private final View rootView;

        @BindView(R.id.loading_spinner)
        View loadingSpinner;

        @BindView(R.id.verse_list)
        RecyclerView verseList;

        private Page(Context context, final VersePagerPresenter versePagerPresenter,
                     VerseSelectionListener listener, View rootView) {
            this.listener = listener;
            this.rootView = rootView;
            ButterKnife.bind(this, rootView);

            verseList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
            verseList.setItemAnimator(new VerseItemAnimator());

            verseListAdapter = new VerseListAdapter(context, versePagerPresenter);
            verseList.setAdapter(verseListAdapter);

            verseList.addOnChildAttachStateChangeListener(this);
            verseList.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);

                    if (inUse && newState == RecyclerView.SCROLL_STATE_IDLE) {
                        final LinearLayoutManager linearLayoutManager
                                = (LinearLayoutManager) recyclerView.getLayoutManager();
                        versePagerPresenter.saveReadingProgress(book, chapter,
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
    private final VersePagerPresenter versePagerPresenter;
    private final LayoutInflater inflater;
    private final ArrayList<Page> pages;

    private VerseSelectionListener listener;

    private int currentBook;
    private int currentChapter;
    private int currentVerse;

    VersePagerAdapter(Context context, VersePagerPresenter versePagerPresenter, int offScreenPageLimit) {
        this.context = context;
        this.versePagerPresenter = versePagerPresenter;
        this.inflater = LayoutInflater.from(context);
        this.pages = new ArrayList<>(1 + 2 * offScreenPageLimit);
    }

    @Override
    public int getCount() {
        return listener == null || TextUtils.isEmpty(versePagerPresenter.loadCurrentTranslation())
                ? 0 : Bible.getChapterCount(currentBook);
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
            page = new Page(context, versePagerPresenter, listener,
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
        versePagerPresenter.loadVerses(currentBook, chapter);
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
    public void onVersesLoaded(List<Verse> verses, @Nullable List<Bookmark> bookmarks, @Nullable List<Note> notes) {
        final int chapter = verses.get(0).verseIndex.chapter();
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            if (page.chapter == chapter) {
                AnimationHelper.fadeOut(page.loadingSpinner);
                AnimationHelper.fadeIn(page.verseList);

                page.verseListAdapter.setVerses(verses, bookmarks, notes);

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
                return;
            }
        }
    }

    @Override
    public void onVersesLoadFailed(int book, final int chapter) {
        DialogHelper.showDialog(context, false, R.string.error_failed_to_load,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadVerses(chapter);
                    }
                }, null);
    }

    @Override
    public void onBookmarkAdded(Bookmark bookmark) {
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            if (page.chapter == bookmark.verseIndex().chapter()) {
                page.verseListAdapter.addBookmark(bookmark);
                return;
            }
        }
    }

    @Override
    public void onBookmarkAddFailed(final VerseIndex verseIndex) {
        DialogHelper.showDialog(context, true, R.string.error_unknown_error,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        versePagerPresenter.addBookmark(verseIndex);
                    }
                }, null);
    }

    @Override
    public void onBookmarkRemoved(VerseIndex verseIndex) {
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            if (page.chapter == verseIndex.chapter()) {
                page.verseListAdapter.removeBookmark(verseIndex);
                return;
            }
        }
    }

    @Override
    public void onBookmarkRemoveFailed(final VerseIndex verseIndex) {
        DialogHelper.showDialog(context, true, R.string.error_unknown_error,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        versePagerPresenter.removeBookmark(verseIndex);
                    }
                }, null);
    }

    @Override
    public void onNoteUpdated(Note note) {
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            if (page.chapter == note.verseIndex().chapter()) {
                page.verseListAdapter.updateNote(note);
                return;
            }
        }
    }

    @Override
    public void onNoteUpdateFailed(final VerseIndex verseIndex, final String note) {
        DialogHelper.showDialog(context, true, R.string.error_unknown_error,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        versePagerPresenter.updateNote(verseIndex, note);
                    }
                }, null);
    }

    @Override
    public void onNoteRemoved(VerseIndex verseIndex) {
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            if (page.chapter == verseIndex.chapter()) {
                page.verseListAdapter.removeNote(verseIndex);
                return;
            }
        }
    }

    @Override
    public void onNoteRemoveFailed(final VerseIndex verseIndex) {
        DialogHelper.showDialog(context, true, R.string.error_unknown_error,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        versePagerPresenter.removeNote(verseIndex);
                    }
                }, null);
    }

    @Override
    public void showNote(VerseIndex verseIndex) {
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            if (page.chapter == verseIndex.chapter()) {
                page.verseListAdapter.showNote(verseIndex);
                return;
            }
        }
    }

    @Override
    public void hideNote(VerseIndex verseIndex) {
        final int pageCount = pages.size();
        for (int i = 0; i < pageCount; ++i) {
            final Page page = pages.get(i);
            if (page.chapter == verseIndex.chapter()) {
                page.verseListAdapter.hideNote(verseIndex);
                return;
            }
        }
    }

    @Override
    public void onTranslationUpdated() {
        notifyDataSetChanged();
    }

    void onStart() {
        versePagerPresenter.takeView(this);

        currentBook = versePagerPresenter.loadCurrentBook();
        currentChapter = versePagerPresenter.loadCurrentChapter();
        currentVerse = versePagerPresenter.loadCurrentVerse();
        notifyDataSetChanged();
    }

    void onStop() {
        versePagerPresenter.dropView();
    }

    void setReadingProgress(VerseIndex index) {
        currentBook = index.book();
        currentChapter = index.chapter();
        currentVerse = index.verse();
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
            if (page.chapter == chapter) {
                return page.verseListAdapter.getSelectedVerses();
            }
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
