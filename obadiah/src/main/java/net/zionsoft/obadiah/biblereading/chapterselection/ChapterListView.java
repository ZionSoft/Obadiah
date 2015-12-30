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

package net.zionsoft.obadiah.biblereading.chapterselection;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ExpandableListView;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.domain.Verse;

import java.util.List;

import javax.inject.Inject;

public class ChapterListView extends ExpandableListView implements ChapterView,
        ChapterListAdapter.Listener, ExpandableListView.OnGroupClickListener {
    @Inject
    ChapterPresenter chapterPresenter;

    private int currentBook;
    private int currentChapter;

    private ChapterListAdapter chapterListAdapter;
    private int lastExpandedGroup;

    public ChapterListView(Context context) {
        super(context);
        initialize(context);
    }

    public ChapterListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ChapterListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChapterListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        BibleReadingChapterComponent.Initializer.init(App.getInjectionComponent(context)).inject(this);

        setBackgroundColor(Color.BLACK);
        setDivider(new ColorDrawable(ContextCompat.getColor(context, R.color.dark_gray)));
        setDividerHeight(1);
        setOnGroupClickListener(this);
        setChildDivider(new ColorDrawable(Color.TRANSPARENT));

        chapterListAdapter = new ChapterListAdapter(context, this);
        setAdapter(chapterListAdapter);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        chapterPresenter.takeView(this);
        chapterPresenter.loadBookNamesForCurrentTranslation();

        currentBook = chapterPresenter.loadCurrentBook();
        currentChapter = chapterPresenter.loadCurrentChapter();
        refresh();
    }

    private void refresh() {
        chapterListAdapter.setCurrentChapter(currentBook, currentChapter);
        chapterListAdapter.notifyDataSetChanged();

        lastExpandedGroup = currentBook;
        expandGroup(currentBook);
        setSelectedGroup(currentBook);
    }

    @Override
    protected void onDetachedFromWindow() {
        chapterPresenter.dropView();
        super.onDetachedFromWindow();
    }

    @Override
    public void onBookNamesLoaded(List<String> bookNames) {
        chapterListAdapter.setBookNames(bookNames);
        chapterListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onReadingProgressUpdated(Verse.Index index) {
        if (currentBook == index.book && currentChapter == index.chapter) {
            return;
        }
        currentBook = index.book;
        currentChapter = index.chapter;

        refresh();
    }

    @Override
    public void onChapterSelected(int book, int chapter) {
        currentBook = book;
        currentChapter = chapter;
        chapterPresenter.saveReadingProgress(currentBook, currentChapter, 0);

        chapterListAdapter.setCurrentChapter(currentBook, currentChapter);
        chapterListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        parent.smoothScrollToPosition(groupPosition);
        if (parent.isGroupExpanded(groupPosition)) {
            parent.collapseGroup(groupPosition);
        } else {
            parent.expandGroup(groupPosition);
            if (lastExpandedGroup != groupPosition) {
                parent.collapseGroup(lastExpandedGroup);
                lastExpandedGroup = groupPosition;
            }
        }
        return true;
    }
}
