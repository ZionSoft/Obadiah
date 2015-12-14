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

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.domain.Bible;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.mvp.models.BibleReadingModel;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class VersePagerAdapter extends PagerAdapter {
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
            verseListAdapter.select(verseList.getChildAdapterPosition(v));
            verseListAdapter.notifyDataSetChanged();

            listener.onVersesSelectionChanged(verseListAdapter.hasSelectedVerses());
        }
    }

    @Inject
    BibleReadingModel bibleReadingModel;

    @Inject
    Settings settings;

    private final Context context;
    private final Listener listener;
    private final LayoutInflater inflater;
    private final List<Page> pages;

    private String translationShortName;
    private int currentBook = -1;
    private int currentChapter;
    private int currentVerse;

    VersePagerAdapter(Context context, Listener listener) {
        super();
        App.get(context).getInjectionComponent().inject(this);

        this.context = context;
        this.listener = listener;
        inflater = LayoutInflater.from(context);
        pages = new LinkedList<>();
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
        loadVerses(position, page);

        return page;
    }

    private void loadVerses(final int position, final Page page) {
        bibleReadingModel.loadVerses(translationShortName, currentBook, position)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<Verse>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        DialogHelper.showDialog(context, false, R.string.dialog_retry,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        loadVerses(position, page);
                                    }
                                }, null);
                    }

                    @Override
                    public void onNext(List<Verse> verses) {
                        if (page.position == position) {
                            AnimationHelper.fadeOut(page.loadingSpinner);
                            AnimationHelper.fadeIn(page.verseList);

                            page.verseListAdapter.setVerses(verses);
                            page.verseListAdapter.notifyDataSetChanged();

                            if (currentVerse > 0 && currentChapter == position) {
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
                        }
                    }
                });
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        for (Page page : pages) {
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

    void setTranslationShortName(String translationShortName) {
        this.translationShortName = translationShortName;
    }

    void setSelected(int currentBook, int currentChapter, int currentVerse) {
        this.currentBook = currentBook;
        this.currentChapter = currentChapter;
        this.currentVerse = currentVerse;
    }

    int getCurrentVerse(int chapter) {
        for (Page page : pages) {
            if (page.position == chapter) {
                final LinearLayoutManager linearLayoutManager
                        = (LinearLayoutManager) page.verseList.getLayoutManager();
                return linearLayoutManager.findFirstVisibleItemPosition();
            }
        }
        return 0;
    }

    List<Verse> getSelectedVerses(int chapter) {
        for (Page page : pages) {
            if (page.position == chapter)
                return page.verseListAdapter.getSelectedVerses();
        }
        return null;
    }

    void deselectVerses() {
        for (Page page : pages) {
            page.verseListAdapter.deselectVerses();
            page.verseListAdapter.notifyDataSetChanged();
        }
    }
}
