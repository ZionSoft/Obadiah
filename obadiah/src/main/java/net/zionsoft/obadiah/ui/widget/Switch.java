/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2017 ZionSoft
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

package net.zionsoft.obadiah.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.AttributeSet;

import net.zionsoft.obadiah.R;

public class Switch extends SwitchCompat {
    public Switch(Context context) {
        super(context);
        init(context, null);
    }

    public Switch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public Switch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        setBackgroundResource(R.drawable.background_text);
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.item_height));

        final Resources resources = getResources();
        setPadding(resources.getDimensionPixelSize(R.dimen.paddingLeft), 0,
                resources.getDimensionPixelSize(R.dimen.paddingRight), 0);

        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Switch);

            final int titleResourceId = a.getResourceId(R.styleable.Switch_switchTitle, -1);
            if (titleResourceId != -1) {
                setText(titleResourceId);
            } else {
                final String titleString = a.getString(R.styleable.Switch_switchTitle);
                if (!TextUtils.isEmpty(titleString)) {
                    setText(titleString);
                }
            }

            a.recycle();
        }
    }
}
