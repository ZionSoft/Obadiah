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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;

import java.util.List;

import javax.inject.Inject;

public class OpenSourceLicenseListAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final int textColor;
    private final float textSize;
    private final List<String> licenses;

    @Inject
    Settings settings;

    public OpenSourceLicenseListAdapter(Context context, List<String> licenses) {
        App.get(context).getInjectionComponent().inject(this);

        inflater = LayoutInflater.from(context);
        textColor = settings.getTextColor();
        textSize = context.getResources().getDimensionPixelSize(settings.getTextSize().textSize);

        this.licenses = licenses;
    }

    @Override
    public int getCount() {
        return licenses != null ? licenses.size() : 0;
    }

    @Override
    public String getItem(int position) {
        return licenses != null ? licenses.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final TextView textView;
        if (convertView == null) {
            textView = (TextView) inflater.inflate(R.layout.item_open_source_license, parent, false);
            textView.setTextColor(textColor);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        } else {
            textView = (TextView) convertView;
        }
        textView.setText(getItem(position));
        return textView;
    }
}
