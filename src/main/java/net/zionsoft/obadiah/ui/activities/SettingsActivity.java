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

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
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

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import javax.inject.Inject;

public class SettingsActivity extends ActionBarActivity {
    public static Intent newStartIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    private static final long ANIMATION_DURATION = 300L;

    @Inject
    Settings mSettings;

    private View mRootView;
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
        App.get(this).getInjectionComponent().inject(this);

        initializeUi();
    }

    private void initializeUi() {
        setContentView(R.layout.activity_settings);

        mRootView = getWindow().getDecorView();

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_action_bar);

        mScreenOnSwitch = (SwitchCompat) findViewById(R.id.screen_on_switch);
        mNightModeSwitch = (SwitchCompat) findViewById(R.id.night_mode_switch);
        mRateMeTextView = (TextView) findViewById(R.id.rate_me_text_view);
        mTextSizeTextView = (TextView) findViewById(R.id.text_size_text_view);
        mTextSizeValueTextView = (TextView) findViewById(R.id.text_size_value_text_view);
        mVersionTextView = (TextView) findViewById(R.id.version_text_view);
        mVersionValueTextView = (TextView) findViewById(R.id.version_value_text_view);

        // must call this before setting the listeners, otherwise all the listeners will be
        // immediately triggered
        populateUi(false);

        mScreenOnSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettings.setKeepScreenOn(isChecked);
                populateUi(false);
            }
        });

        mNightModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSettings.setNightMode(isChecked);
                populateUi(true);
            }
        });

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
                                populateUi(false);
                                dialog.dismiss();
                            }
                        });
            }
        });

        try {
            mVersionValueTextView.setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            // do nothing
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void populateUi(boolean animateColor) {
        mRootView.setKeepScreenOn(mSettings.keepScreenOn());

        final int backgroundColor = mSettings.getBackgroundColor();
        final int textColor = mSettings.getTextColor();
        if (animateColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // TODO adds animation for old devices
            ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), textColor, backgroundColor);
            colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    mRootView.setBackgroundColor((Integer) animator.getAnimatedValue());
                }
            });
            colorAnimator.setDuration(ANIMATION_DURATION).start();

            colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), backgroundColor, textColor);
            colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    final int animatedColorValue = (Integer) animator.getAnimatedValue();
                    mScreenOnSwitch.setTextColor(animatedColorValue);
                    mNightModeSwitch.setTextColor(animatedColorValue);
                    mTextSizeTextView.setTextColor(animatedColorValue);
                    mRateMeTextView.setTextColor(animatedColorValue);
                    mVersionTextView.setTextColor(animatedColorValue);
                }
            });
            colorAnimator.setDuration(ANIMATION_DURATION).start();
        } else {
            mRootView.setBackgroundColor(backgroundColor);
            mScreenOnSwitch.setTextColor(textColor);
            mNightModeSwitch.setTextColor(textColor);
            mTextSizeTextView.setTextColor(textColor);
            mRateMeTextView.setTextColor(textColor);
            mVersionTextView.setTextColor(textColor);
        }

        final Settings.TextSize textSizeSetting = mSettings.getTextSize();
        final Resources resources = getResources();
        final float textSize = resources.getDimension(textSizeSetting.textSize);
        final float smallerTextSize = resources.getDimension(textSizeSetting.smallerTextSize);

        mScreenOnSwitch.setChecked(mSettings.keepScreenOn());
        mScreenOnSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mNightModeSwitch.setChecked(mSettings.isNightMode());
        mNightModeSwitch.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        mTextSizeTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        mTextSizeValueTextView.setText(textSizeSetting.title);
        mTextSizeValueTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        mRateMeTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
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
