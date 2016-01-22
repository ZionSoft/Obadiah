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

package net.zionsoft.obadiah.bookmarks;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.Verse;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

class BookmarksListAdapter extends RecyclerView.Adapter {
    static class ViewHolder extends RecyclerView.ViewHolder {
        private static final StringBuilder STRING_BUILDER = new StringBuilder();

        @Bind(R.id.text)
        TextView text;

        private ViewHolder(View itemView, BookmarksPresenter bookmarksPresenter, Resources resources) {
            super(itemView);

            ButterKnife.bind(this, itemView);
            final Settings settings = bookmarksPresenter.getSettings();
            text.setTextColor(settings.getTextColor());
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(settings.getTextSize().textSize));
        }

        private void bind(Bookmark bookmark, Verse verse) {
            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(verse.text.bookName).append(' ')
                    .append(verse.verseIndex.chapter + 1).append(':').append(verse.verseIndex.verse + 1)
                    .append('\n').append(verse.text.text);
            text.setText(STRING_BUILDER.toString());
        }
    }

    private final BookmarksPresenter bookmarksPresenter;
    private final LayoutInflater inflater;
    private final Resources resources;

    private List<Bookmark> bookmarks;
    private List<Verse> verses;

    BookmarksListAdapter(Context context, BookmarksPresenter bookmarksPresenter) {
        this.bookmarksPresenter = bookmarksPresenter;
        this.inflater = LayoutInflater.from(context);
        this.resources = context.getResources();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflater.inflate(R.layout.item_bookmark, parent, false),
                bookmarksPresenter, resources);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder) holder).bind(bookmarks.get(position), verses.get(position));
    }

    @Override
    public int getItemCount() {
        return bookmarks != null ? bookmarks.size() : 0;
    }

    void setBookmarks(List<Bookmark> bookmarks, List<Verse> verses) {
        this.bookmarks = bookmarks;
        this.verses = verses;
        notifyDataSetChanged();
    }

    Verse getVerse(int position) {
        return verses.get(position);
    }
}
