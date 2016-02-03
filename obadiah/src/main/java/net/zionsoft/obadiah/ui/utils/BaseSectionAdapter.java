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

package net.zionsoft.obadiah.ui.utils;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.ui.widget.SectionHeader;

import java.util.ArrayList;

public abstract class BaseSectionAdapter<Item> extends RecyclerView.Adapter {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    protected final LayoutInflater inflater;
    protected final int textColor;
    protected final float textSize;
    protected final float smallerTextSize;

    private ArrayList<String> headers;
    private ArrayList<ArrayList<Item>> groupedItems;
    private int count;

    protected BaseSectionAdapter(Context context, Settings settings) {
        this.inflater = LayoutInflater.from(context);
        this.textColor = settings.getTextColor();
        final Resources resources = context.getResources();
        final Settings.TextSize textSize = settings.getTextSize();
        this.textSize = resources.getDimension(textSize.textSize);
        this.smallerTextSize = resources.getDimension(textSize.smallerTextSize);
    }

    @Override
    public int getItemCount() {
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        }

        final int size = groupedItems.size();
        for (int i = 0; i < size; ++i) {
            position -= groupedItems.get(i).size() + 1;
            if (position < 0) {
                return VIEW_TYPE_ITEM;
            } else if (position == 0) {
                return VIEW_TYPE_HEADER;
            }
        }

        throw new IllegalStateException("Unknown view type for position - " + position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                return createHeaderViewHolder(parent);
            case VIEW_TYPE_ITEM:
                return createItemViewHolder(parent);
            default:
                throw new IllegalStateException("Unknown view type - " + viewType);
        }
    }

    private RecyclerView.ViewHolder createHeaderViewHolder(ViewGroup parent) {
        final SectionHeader header = (SectionHeader)
                inflater.inflate(R.layout.item_section_header, parent, false);
        header.setHeaderTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        return new RecyclerView.ViewHolder(header) {
        };
    }

    abstract protected RecyclerView.ViewHolder createItemViewHolder(ViewGroup parent);

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position == 0) {
            // VIEW_TYPE_HEADER
            bindHeaderViewHeader(holder, headers.get(0));
            return;
        }

        final int groupedItemsSize = groupedItems.size();
        for (int i = 0; i < groupedItemsSize; ++i) {
            final ArrayList<Item> items = groupedItems.get(i);
            --position;
            final int itemsSize = items.size();
            if (position < itemsSize) {
                // VIEW_TYPE_ITEM
                bindItemViewHeader(holder, items.get(position));
                return;
            }

            position -= itemsSize;
            if (position == 0) {
                // VIEW_TYPE_HEADER
                bindHeaderViewHeader(holder, headers.get(i + 1));
                return;
            }
        }

        throw new IllegalStateException("Unknown view type for position - " + position);
    }

    private void bindHeaderViewHeader(RecyclerView.ViewHolder holder, String header) {
        ((SectionHeader) holder.itemView).setHeaderText(header);
    }

    abstract protected void bindItemViewHeader(RecyclerView.ViewHolder holder, Item item);

    protected void setData(ArrayList<String> headers, ArrayList<ArrayList<Item>> groupedItems, int count) {
        this.headers = headers;
        this.groupedItems = groupedItems;
        this.count = count;

        notifyDataSetChanged();
    }

    @Nullable
    public Item getItem(int position) {
        if (position == 0) {
            // VIEW_TYPE_HEADER
            return null;
        }

        final int groupedItemsSize = groupedItems.size();
        for (int i = 0; i < groupedItemsSize; ++i) {
            final ArrayList<Item> items = groupedItems.get(i);
            --position;
            final int itemsSize = items.size();
            if (position < itemsSize) {
                // VIEW_TYPE_ITEM
                return items.get(position);
            }

            position -= itemsSize;
            if (position == 0) {
                // VIEW_TYPE_HEADER
                return null;
            }
        }

        throw new IllegalStateException("Unknown view type for position - " + position);
    }
}
