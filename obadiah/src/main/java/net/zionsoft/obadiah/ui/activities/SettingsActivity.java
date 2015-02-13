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
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.ui.utils.DialogHelper;
import net.zionsoft.obadiah.ui.widget.settings.SettingSectionHeader;
import net.zionsoft.obadiah.ui.widget.settings.SettingSwitch;
import net.zionsoft.obadiah.ui.widget.settings.SettingTitleDescriptionButton;

import javax.inject.Inject;

import butterknife.InjectView;

public class SettingsActivity extends BaseActionBarActivity {
    public static Intent newStartIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    private static final long ANIMATION_DURATION = 300L;

    @Inject
    Settings settings;

    @InjectView(R.id.display_section_header)
    SettingSectionHeader displaySectionHeader;

    @InjectView(R.id.screen_on_switch)
    SettingSwitch screenOnSwitch;

    @InjectView(R.id.night_mode_switch)
    SettingSwitch nightModeSwitch;

    @InjectView(R.id.text_size_setting_button)
    SettingTitleDescriptionButton textSizeSettingButton;

    @InjectView(R.id.about_section_header)
    SettingSectionHeader aboutSectionHeader;

    @InjectView(R.id.rate_me_setting_button)
    SettingTitleDescriptionButton rateMeSettingButton;

    @InjectView(R.id.version_setting_button)
    SettingTitleDescriptionButton versionSettingButton;

    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.get(this).getInjectionComponent().inject(this);

        initializeUi();
    }

    private void initializeUi() {
        setContentView(R.layout.activity_settings);

        rootView = getWindow().getDecorView();

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_action_bar);

        // must call this before setting the listeners, otherwise all the listeners will be
        // immediately triggered
        populateUi(false);

        screenOnSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setKeepScreenOn(isChecked);
                populateUi(false);
            }
        });

        nightModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setNightMode(isChecked);
                populateUi(true);
            }
        });

        rateMeSettingButton.setOnClickListener(new View.OnClickListener() {
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

        textSizeSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelper.showDialog(SettingsActivity.this, R.string.pref_text_size_dialog_title,
                        R.array.pref_text_size_value, settings.getTextSize().ordinal(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                settings.setTextSize(Settings.TextSize.values()[which]);
                                populateUi(false);
                                dialog.dismiss();
                            }
                        });
            }
        });

        try {
            versionSettingButton.setDescriptionText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            // do nothing
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void populateUi(boolean animateColor) {
        rootView.setKeepScreenOn(settings.keepScreenOn());

        final int backgroundColor = settings.getBackgroundColor();
        final int textColor = settings.getTextColor();
        if (animateColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // TODO adds animation for old devices
            ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), textColor, backgroundColor);
            colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    rootView.setBackgroundColor((Integer) animator.getAnimatedValue());
                }
            });
            colorAnimator.setDuration(ANIMATION_DURATION).start();

            colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), backgroundColor, textColor);
            colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    final int animatedColorValue = (Integer) animator.getAnimatedValue();
                    screenOnSwitch.setTitleTextColor(animatedColorValue);
                    nightModeSwitch.setTitleTextColor(animatedColorValue);
                    textSizeSettingButton.setTitleTextColor(animatedColorValue);
                    rateMeSettingButton.setTitleTextColor(animatedColorValue);
                    versionSettingButton.setTitleTextColor(animatedColorValue);
                }
            });
            colorAnimator.setDuration(ANIMATION_DURATION).start();
        } else {
            rootView.setBackgroundColor(backgroundColor);
            screenOnSwitch.setTitleTextColor(textColor);
            nightModeSwitch.setTitleTextColor(textColor);
            textSizeSettingButton.setTitleTextColor(textColor);
            rateMeSettingButton.setTitleTextColor(textColor);
            versionSettingButton.setTitleTextColor(textColor);
        }

        final Settings.TextSize textSizeSetting = settings.getTextSize();
        final Resources resources = getResources();
        final float textSize = resources.getDimension(textSizeSetting.textSize);
        final float smallerTextSize = resources.getDimension(textSizeSetting.smallerTextSize);

        displaySectionHeader.setHeaderTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        screenOnSwitch.setChecked(settings.keepScreenOn());
        screenOnSwitch.setTitleTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        nightModeSwitch.setChecked(settings.isNightMode());
        nightModeSwitch.setTitleTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        textSizeSettingButton.setDescriptionText(textSizeSetting.title);
        textSizeSettingButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);

        rateMeSettingButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);
        aboutSectionHeader.setHeaderTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
        versionSettingButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);
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
