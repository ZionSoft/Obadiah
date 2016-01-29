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
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseIndex;

import java.util.ArrayList;
import java.util.List;

class VerseListAdapter extends RecyclerView.Adapter<VerseItemViewHolder> {
    private final VersePagerPresenter versePagerPresenter;
    private final LayoutInflater inflater;
    private final Resources resources;

    private List<Bookmark> bookmarks;
    private List<Verse> verses;
    private boolean[] selected;
    private int selectedCount;

    VerseListAdapter(Context context, VersePagerPresenter versePagerPresenter) {
        this.versePagerPresenter = versePagerPresenter;
        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
    }

    @Override
    public int getItemCount() {
        return verses != null ? verses.size() : 0;
    }

    @Override
    public VerseItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VerseItemViewHolder(inflater, parent, versePagerPresenter, resources);
    }

    @Override
    public void onBindViewHolder(VerseItemViewHolder holder, int position) {
        boolean isBookmarked = false;
        final int bookmarkCount = bookmarks.size();
        for (int i = 0; i < bookmarkCount; ++i) {
            final VerseIndex verseIndex = bookmarks.get(i).verseIndex;
            if (verseIndex.verse == position) {
                isBookmarked = true;
                break;
            }
        }
        holder.bind(verses.get(position), selected[position], isBookmarked);
    }

    @Override
    public void onBindViewHolder(VerseItemViewHolder holder, int position, List<Object> payloads) {
        if (payloads.size() == 0) {
            onBindViewHolder(holder, position);
        }
        // if there are payloads, the view will be updated by VerseItemAnimator
    }

    void setVerses(List<Verse> verses, List<Bookmark> bookmarks) {
        this.verses = verses;
        this.bookmarks = bookmarks;

        final int size = this.verses.size();
        if (selected == null || selected.length < size) {
            selected = new boolean[size];
        }
        deselectVerses();

        notifyDataSetChanged();
    }

    void addBookmark(Bookmark bookmark) {
        bookmarks.add(bookmark);
        notifyItemChanged(bookmark.verseIndex.verse, VerseItemAnimator.VerseItemHolderInfo.ACTION_ADD_BOOKMARK);
    }

    void removeBookmark(VerseIndex verseIndex) {
        final int bookmarkCount = bookmarks.size();
        for (int i = 0; i < bookmarkCount; ++i) {
            if (bookmarks.get(i).verseIndex.equals(verseIndex)) {
                bookmarks.remove(i);
                notifyItemChanged(verseIndex.verse, VerseItemAnimator.VerseItemHolderInfo.ACTION_REMOVE_BOOKMARK);
                return;
            }
        }
    }

    void select(int position) {
        selected[position] ^= true;
        if (selected[position]) {
            ++selectedCount;
        } else {
            --selectedCount;
        }
    }

    boolean hasSelectedVerses() {
        return selectedCount > 0;
    }

    List<Verse> getSelectedVerses() {
        final List<Verse> selectedVerses = new ArrayList<>(selectedCount);
        for (int i = 0; i < selected.length; ++i) {
            if (selected[i]) {
                selectedVerses.add(verses.get(i));
            }
        }
        return selectedVerses;
    }

    void deselectVerses() {
        if (selected != null) {
            for (int i = 0; i < selected.length; ++i) {
                selected[i] = false;
            }
        }
        selectedCount = 0;
    }
}
