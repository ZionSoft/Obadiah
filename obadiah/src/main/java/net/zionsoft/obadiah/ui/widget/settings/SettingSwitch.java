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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import net.zionsoft.obadiah.R;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SettingSwitch extends FrameLayout {
    @Bind(R.id.switch_button)
    SwitchCompat switchButton;

    @Bind(R.id.divider_view)
    View dividerView;

    public SettingSwitch(Context context) {
        super(context);
        init(context, null);
    }

    public SettingSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SettingSwitch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SettingSwitch(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        inflate(context, R.layout.item_setting_switch, this);
        ButterKnife.bind(this, this);

        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SettingSwitch);

            final int titleResourceId = a.getResourceId(R.styleable.SettingSwitch_settingSwitchTitle, -1);
            if (titleResourceId != -1) {
                switchButton.setText(titleResourceId);
            } else {
                final String titleString = a.getString(R.styleable.SettingSwitch_settingSwitchTitle);
                if (!TextUtils.isEmpty(titleString)) {
                    switchButton.setText(titleString);
                }
            }

            dividerView.setVisibility(a.getBoolean(R.styleable.SettingSwitch_settingSwitchDividerVisible, true) ? VISIBLE : GONE);

            a.recycle();
        }
    }

    public void setTitleTextColor(int color) {
        switchButton.setTextColor(color);
    }

    public void setTitleTextSize(int unit, float titleTextSize) {
        switchButton.setTextSize(unit, titleTextSize);
    }

    public void setChecked(boolean checked) {
        switchButton.setChecked(checked);
    }

    public void setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        switchButton.setOnCheckedChangeListener(listener);
    }
}
