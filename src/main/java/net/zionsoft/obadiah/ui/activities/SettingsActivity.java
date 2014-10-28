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

package net.zionsoft.obadiah.ui.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

public class SettingsActivity extends ActionBarActivity {
    public static Intent newStartIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    private Settings mSettings;

    private SwitchCompat mScreenOnSwitch;
    private SwitchCompat mNightModeSwitch;
    private TextView mTextSizeTextView;
    private TextView mTextSizeValueTextView;
    private TextView mRateMeTextView;
    private TextView mVersionTextView;
    private TextView mVersionValueTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = Settings.getInstance();

        initializeUi();
        populateUi();
    }

    private void initializeUi() {
        setContentView(R.layout.activity_settings);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_action_bar);

        mScreenOnSwitch = (SwitchCompat) findViewById(R.id.screen_on_switch);
        mScreenOnSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettings.setKeepScreenOn(isChecked);
                populateUi();
            }
        });

        mNightModeSwitch = (SwitchCompat) findViewById(R.id.night_mode_switch);
        mNightModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettings.setNightMode(isChecked);
                populateUi();
            }
        });

        mRateMeTextView = (TextView) findViewById(R.id.rate_me_text_view);
        mRateMeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Constants.GOOGLE_PLAY_URI));
                    Analytics.trackUIEvent("rate_app");
                } catch (ActivityNotFoundException e) {
                    Analytics.trackException("Failed to open market for rating: "
                            + Build.MANUFACTURER + ", " + Build.MODEL);

                    DialogHelper.showDialog(SettingsActivity.this, R.string.dialog_unknown_error, null);
                }
            }
        });

        findViewById(R.id.text_size_section).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelper.showDialog(SettingsActivity.this, R.string.pref_text_size_dialog_title,
                        R.array.pref_text_size_value, mSettings.getTextSize().ordinal(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mSettings.setTextSize(Settings.TextSize.values()[which]);
                                populateUi();
                                dialog.dismiss();
                            }
                        });
            }
        });
        mTextSizeTextView = (TextView) findViewById(R.id.text_size_text_view);
        mTextSizeValueTextView = (TextView) findViewById(R.id.text_size_value_text_view);

        mVersionTextView = (TextView) findViewById(R.id.version_text_view);
        mVersionValueTextView = (TextView) findViewById(R.id.version_value_text_view);
        try {
            mVersionValueTextView.setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            // do nothing
        }
    }

    private void populateUi() {
        final View rootView = getWindow().getDecorView();
        rootView.setKeepScreenOn(mSettings.keepScreenOn());
        rootView.setBackgroundColor(mSettings.getBackgroundColor());

        final int textColor = mSettings.getTextColor();
        final Settings.TextSize textSizeSetting = mSettings.getTextSize();
        final Resources resources = getResources();
        final float textSize = resources.getDimension(textSizeSetting.textSize);
        final float smallerTextSize = resources.getDimension(textSizeSetting.smallerTextSize);

        mScreenOnSwitch.setChecked(mSettings.keepScreenOn());
        mScreenOnSwitch.setTextColor(textColor);
        mScreenOnSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mNightModeSwitch.setChecked(mSettings.isNightMode());
        mNightModeSwitch.setTextColor(textColor);
        mNightModeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mTextSizeTextView.setTextColor(textColor);
        mTextSizeTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mTextSizeValueTextView.setText(textSizeSetting.title);
        mTextSizeValueTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        mRateMeTextView.setTextColor(textColor);
        mRateMeTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mVersionTextView.setTextColor(textColor);
        mVersionTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mVersionValueTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Analytics.trackScreen(SettingsActivity.class.getSimpleName());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left_to_right);
    }
}
