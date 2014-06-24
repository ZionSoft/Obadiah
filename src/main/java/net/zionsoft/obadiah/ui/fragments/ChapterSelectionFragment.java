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

package net.zionsoft.obadiah.ui.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.ui.adapters.BookExpandableListAdapter;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

public class ChapterSelectionFragment extends Fragment {
    public static interface Listener {
        public void onChapterSelected(int bookIndex, int chapterIndex);
    }

    private Bible mBible;
    private Listener mListener;

    private int mCurrentBook;
    private int mCurrentChapter;

    private BookExpandableListAdapter mBookListAdapter;
    private ExpandableListView mBookListView;
    private int mLastExpandedGroup;

    public ChapterSelectionFragment() {
        super();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        setRetainInstance(true);
        mBible = Bible.getInstance();
        mListener = (Listener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chapter_selection, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBookListAdapter = new BookExpandableListAdapter(getActivity(),
                new BookExpandableListAdapter.OnChapterClickListener() {
                    @Override
                    public void onChapterClicked(int book, int chapter) {
                        if (mCurrentBook == book && mCurrentChapter == chapter)
                            return;

                        mCurrentBook = book;
                        mCurrentChapter = chapter;

                        mBookListAdapter.setSelected(mCurrentBook, mCurrentChapter);
                        mBookListAdapter.notifyDataSetChanged();

                        if (mListener != null)
                            mListener.onChapterSelected(mCurrentBook, mCurrentChapter);
                    }
                }
        );

        mBookListView = (ExpandableListView) view.findViewById(R.id.book_list_view);
        mBookListView.setAdapter(mBookListAdapter);
        mBookListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                parent.smoothScrollToPosition(groupPosition);
                if (parent.isGroupExpanded(groupPosition)) {
                    parent.collapseGroup(groupPosition);
                } else {
                    parent.expandGroup(groupPosition);
                    parent.collapseGroup(mLastExpandedGroup);
                    mLastExpandedGroup = groupPosition;
                }
                return true;
            }
        });
    }

    @Override
    public void onDetach() {
        mListener = null;

        super.onDetach();
    }

    public void setSelected(int currentBook, int currentChapter) {
        mCurrentBook = currentBook;
        mCurrentChapter = currentChapter;

        mBookListAdapter.setSelected(currentBook, currentChapter);
        mBookListAdapter.notifyDataSetChanged();

        mLastExpandedGroup = mCurrentBook;
        mBookListView.expandGroup(mCurrentBook);
        mBookListView.setSelectedGroup(mCurrentBook);
    }

    public void setSelected(String translationShortName, int currentBook, int currentChapter) {
        setSelected(currentBook, currentChapter);
        loadBookNames(translationShortName);
    }

    private void loadBookNames(final String translationShortName) {
        mBible.loadBookNames(translationShortName, new Bible.OnStringsLoadedListener() {
                    @Override
                    public void onStringsLoaded(List<String> strings) {
                        if (!isAdded())
                            return;

                        if (strings == null || strings.size() == 0) {
                            DialogHelper.showDialog(getActivity(), false, R.string.dialog_retry,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            loadBookNames(translationShortName);
                                        }
                                    }, null
                            );
                            return;
                        }

                        mBookListAdapter.setSelected(mCurrentBook, mCurrentChapter);
                        mBookListAdapter.setBookNames(strings);
                        mBookListAdapter.notifyDataSetChanged();

                        mLastExpandedGroup = mCurrentBook;
                        mBookListView.expandGroup(mCurrentBook);
                        mBookListView.setSelectedGroup(mCurrentBook);
                    }
                }
        );
    }
}
