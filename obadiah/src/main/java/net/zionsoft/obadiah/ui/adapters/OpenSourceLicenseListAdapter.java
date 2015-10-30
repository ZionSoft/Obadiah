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

package net.zionsoft.obadiah.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;

import java.util.List;

public class OpenSourceLicenseListAdapter extends RecyclerView.Adapter {
    private final LayoutInflater inflater;
    private final int textColor;
    private final float textSize;
    private final List<String> licenses;

    public OpenSourceLicenseListAdapter(Context context, Settings settings, List<String> licenses) {
        this.inflater = LayoutInflater.from(context);
        this.textColor = settings.getTextColor();
        this.textSize = context.getResources().getDimensionPixelSize(settings.getTextSize().textSize);
        this.licenses = licenses;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView textView = (TextView) inflater.inflate(R.layout.item_open_source_license, parent, false);
        textView.setTextColor(textColor);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        return new RecyclerView.ViewHolder(textView) {
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((TextView) holder.itemView).setText(licenses.get(position));
    }

    @Override
    public int getItemCount() {
        return licenses.size();
    }
}
