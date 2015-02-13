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

package net.zionsoft.obadiah.ui.widget.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.zionsoft.obadiah.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class SettingSectionHeader extends FrameLayout {
    @InjectView(R.id.header_text_view)
    TextView headerTextView;

    public SettingSectionHeader(Context context) {
        super(context);
        init(context, null);
    }

    public SettingSectionHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SettingSectionHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        inflate(context, R.layout.header_setting_section, this);
        ButterKnife.inject(this, this);

        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SettingSectionHeader);
            final int headerResourceId = a.getResourceId(R.styleable.SettingSectionHeader_settingSectionHeaderText, -1);
            if (headerResourceId != -1) {
                headerTextView.setText(headerResourceId);
            } else {
                final String headerText = a.getString(R.styleable.SettingSectionHeader_settingSectionHeaderText);
                if (!TextUtils.isEmpty(headerText)) {
                    headerTextView.setText(headerText);
                }
            }
            a.recycle();
        }
    }

    public void setHeaderTextSize(int unit, float size) {
        headerTextView.setTextSize(unit, size);
    }
}