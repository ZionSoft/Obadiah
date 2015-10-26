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
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;
import net.zionsoft.obadiah.ui.widget.settings.SettingSectionHeader;
import net.zionsoft.obadiah.ui.widget.settings.SettingSwitch;
import net.zionsoft.obadiah.ui.widget.settings.SettingTitleDescriptionButton;

import javax.inject.Inject;

import butterknife.Bind;

public class SettingsActivity extends BaseAppCompatActivity {
    public static Intent newStartIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    private static final long ANIMATION_DURATION = 300L;

    @Inject
    Settings settings;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.display_section_header)
    SettingSectionHeader displaySectionHeader;

    @Bind(R.id.screen_on_switch)
    SettingSwitch screenOnSwitch;

    @Bind(R.id.night_mode_switch)
    SettingSwitch nightModeSwitch;

    @Bind(R.id.text_size_setting_button)
    SettingTitleDescriptionButton textSizeSettingButton;

    @Bind(R.id.about_section_header)
    SettingSectionHeader aboutSectionHeader;

    @Bind(R.id.rate_me_setting_button)
    SettingTitleDescriptionButton rateMeSettingButton;

    @Bind(R.id.version_setting_button)
    SettingTitleDescriptionButton versionSettingButton;

    @Bind(R.id.license_setting_button)
    SettingTitleDescriptionButton licenseSettingButton;

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

        toolbar.setLogo(R.drawable.ic_action_bar);
        toolbar.setTitle(R.string.activity_settings);
        setSupportActionBar(toolbar);

        rootView.setKeepScreenOn(settings.keepScreenOn());
        updateColor();
        updateTextSize();

        screenOnSwitch.setChecked(settings.keepScreenOn());
        screenOnSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setKeepScreenOn(isChecked);
            }
        });

        nightModeSwitch.setChecked(settings.isNightMode());
        nightModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                final int originalBackgroundColor = settings.getBackgroundColor();
                final int originalTextColor = settings.getTextColor();

                settings.setNightMode(isChecked);

                animateColor(originalBackgroundColor, settings.getBackgroundColor(),
                        originalTextColor, settings.getTextColor());
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

        textSizeSettingButton.setDescriptionText(settings.getTextSize().title);
        textSizeSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogHelper.showDialog(SettingsActivity.this, R.string.pref_text_size_dialog_title,
                        R.array.pref_text_size_value, settings.getTextSize().ordinal(),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final Settings.TextSize originalTextSizeSetting = settings.getTextSize();
                                final Resources resources = getResources();
                                final float originalTextSize = resources.getDimension(originalTextSizeSetting.textSize);
                                final float originalSmallerTextSize = resources.getDimension(originalTextSizeSetting.smallerTextSize);

                                settings.setTextSize(Settings.TextSize.values()[which]);
                                textSizeSettingButton.setDescriptionText(settings.getTextSize().title);

                                final Settings.TextSize textSizeSetting = settings.getTextSize();
                                animateTextSize(originalTextSize, resources.getDimension(textSizeSetting.textSize),
                                        originalSmallerTextSize, resources.getDimension(textSizeSetting.smallerTextSize));

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

        licenseSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimationHelper.slideIn(SettingsActivity.this,
                        OpenSourceLicenseActivity.newStartIntent(SettingsActivity.this));
            }
        });
    }

    private void animateColor(final int fromBackgroundColor, final int toBackgroundColor,
                              final int fromTitleTextColor, final int toTitleTextColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
            final ValueAnimator colorAnimator = ValueAnimator.ofFloat(0.0F, 1.0F);
            colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    final float fraction = (Float) animator.getAnimatedValue();
                    final int backgroundColor = (Integer) argbEvaluator.evaluate(fraction, fromBackgroundColor, toBackgroundColor);
                    final int titleTextColor = (Integer) argbEvaluator.evaluate(fraction, fromTitleTextColor, toTitleTextColor);
                    updateColor(backgroundColor, titleTextColor);
                }
            });
            colorAnimator.setDuration(ANIMATION_DURATION).start();
        } else {
            // TODO adds animation for old devices
            updateColor(toBackgroundColor, toTitleTextColor);
        }
    }

    private void updateColor() {
        updateColor(settings.getBackgroundColor(), settings.getTextColor());
    }

    private void updateColor(int backgroundColor, int titleTextColor) {
        rootView.setBackgroundColor(backgroundColor);

        screenOnSwitch.setTitleTextColor(titleTextColor);
        nightModeSwitch.setTitleTextColor(titleTextColor);
        textSizeSettingButton.setTitleTextColor(titleTextColor);
        rateMeSettingButton.setTitleTextColor(titleTextColor);
        versionSettingButton.setTitleTextColor(titleTextColor);
        licenseSettingButton.setTitleTextColor(titleTextColor);
    }

    private void animateTextSize(final float fromTextSize, final float toTextSize,
                                 final float fromSmallerTextSize, final float toSmallerTextSize) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            final ValueAnimator textSizeAnimator = ValueAnimator.ofFloat(0.0F, 1.0F);
            textSizeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    final float fraction = (Float) animator.getAnimatedValue();
                    final float textSize = fromTextSize + fraction * (toTextSize - fromTextSize);
                    final float smallerTextSize = fromSmallerTextSize + fraction * (toSmallerTextSize - fromSmallerTextSize);
                    updateTextSize(textSize, smallerTextSize);
                }
            });
            textSizeAnimator.setDuration(ANIMATION_DURATION).start();
        } else {
            // TODO adds animation for old devices
            updateTextSize(toTextSize, toSmallerTextSize);
        }
    }

    private void updateTextSize() {
        final Settings.TextSize textSizeSetting = settings.getTextSize();
        final Resources resources = getResources();
        updateTextSize(resources.getDimension(textSizeSetting.textSize),
                resources.getDimension(textSizeSetting.smallerTextSize));
    }

    private void updateTextSize(float textSize, float smallerTextSize) {
        displaySectionHeader.setHeaderTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);

        screenOnSwitch.setTitleTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        nightModeSwitch.setTitleTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        textSizeSettingButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);

        rateMeSettingButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);
        aboutSectionHeader.setHeaderTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
        versionSettingButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);
        licenseSettingButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);
    }
}
