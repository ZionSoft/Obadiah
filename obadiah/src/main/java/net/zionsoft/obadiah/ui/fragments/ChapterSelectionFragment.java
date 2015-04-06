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

package net.zionsoft.obadiah.ui.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.ui.adapters.BookExpandableListAdapter;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

import javax.inject.Inject;

import butterknife.InjectView;

public class ChapterSelectionFragment extends BaseFragment {
    public interface Listener {
        public void onChapterSelected(int bookIndex, int chapterIndex);
    }

    @Inject
    Bible bible;

    @InjectView(R.id.book_list_view)
    ExpandableListView bookListView;

    private Listener listener;

    private int currentBook;
    private int currentChapter;

    private BookExpandableListAdapter bookListAdapter;
    private int lastExpandedGroup;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        setRetainInstance(true);
        listener = (Listener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chapter_selection, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        App.get(getActivity()).getInjectionComponent().inject(this);

        bookListAdapter = new BookExpandableListAdapter(getActivity(),
                new BookExpandableListAdapter.OnChapterClickListener() {
                    @Override
                    public void onChapterClicked(int book, int chapter) {
                        if (currentBook == book && currentChapter == chapter)
                            return;

                        currentBook = book;
                        currentChapter = chapter;

                        bookListAdapter.setSelected(currentBook, currentChapter);
                        bookListAdapter.notifyDataSetChanged();

                        if (listener != null)
                            listener.onChapterSelected(currentBook, currentChapter);
                    }
                }
        );

        bookListView.setAdapter(bookListAdapter);
        bookListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
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
        });
    }

    @Override
    public void onDetach() {
        listener = null;

        super.onDetach();
    }

    public void setSelected(int currentBook, int currentChapter) {
        this.currentBook = currentBook;
        this.currentChapter = currentChapter;

        bookListAdapter.setSelected(currentBook, currentChapter);
        bookListAdapter.notifyDataSetChanged();

        lastExpandedGroup = this.currentBook;
        bookListView.expandGroup(this.currentBook);
        bookListView.setSelectedGroup(this.currentBook);
    }

    public void setSelected(String translationShortName, int currentBook, int currentChapter) {
        setSelected(currentBook, currentChapter);
        loadBookNames(translationShortName);
    }

    private void loadBookNames(final String translationShortName) {
        bible.loadBookNames(translationShortName, new Bible.OnStringsLoadedListener() {
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

                        bookListAdapter.setSelected(currentBook, currentChapter);
                        bookListAdapter.setBookNames(strings);
                        bookListAdapter.notifyDataSetChanged();

                        lastExpandedGroup = currentBook;
                        bookListView.expandGroup(currentBook);
                        bookListView.setSelectedGroup(currentBook);
                    }
                }
        );
    }
}
