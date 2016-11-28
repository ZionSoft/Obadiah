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

package net.zionsoft.obadiah.misc.settings;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;

import com.google.android.gms.appinvite.AppInviteInvitation;

import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.misc.license.OpenSourceLicenseActivity;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.User;
import net.zionsoft.obadiah.ui.utils.BaseAppCompatActivity;
import net.zionsoft.obadiah.ui.utils.DialogHelper;
import net.zionsoft.obadiah.ui.widget.SectionHeader;

import javax.inject.Inject;

import butterknife.BindView;

public class SettingsActivity extends BaseAppCompatActivity implements SettingsView {
    public static Intent newStartIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    private static final long ANIMATION_DURATION = 300L;
    private static final int REQUEST_CODE_INVITE_FRIENDS = 8964;
    private static final int REQUEST_CODE_LOGIN = 8965;

    @Inject
    SettingsPresenter settingsPresenter;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.account_section_header)
    SectionHeader accountSectionHeader;

    @BindView(R.id.login_button)
    SettingTitleDescriptionButton loginButton;

    @BindView(R.id.account_button)
    SettingTitleDescriptionButton accountButton;

    @BindView(R.id.logout_button)
    SettingTitleDescriptionButton logoutButton;

    @BindView(R.id.reading_section_header)
    SectionHeader readingSectionHeader;

    @BindView(R.id.simple_reading_switch)
    SettingSwitch simpleReadingSwitch;

    @BindView(R.id.display_section_header)
    SectionHeader displaySectionHeader;

    @BindView(R.id.screen_on_switch)
    SettingSwitch screenOnSwitch;

    @BindView(R.id.night_mode_switch)
    SettingSwitch nightModeSwitch;

    @BindView(R.id.text_size_setting_button)
    SettingTitleDescriptionButton textSizeSettingButton;

    @BindView(R.id.about_section_header)
    SectionHeader aboutSectionHeader;

    @BindView(R.id.rate_me_setting_button)
    SettingTitleDescriptionButton rateMeSettingButton;

    @BindView(R.id.invite_friends_setting_button)
    SettingTitleDescriptionButton inviteFriendsSettingButton;

    @BindView(R.id.version_setting_button)
    SettingTitleDescriptionButton versionSettingButton;

    @BindView(R.id.license_setting_button)
    SettingTitleDescriptionButton licenseSettingButton;

    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final FragmentManager fm = getSupportFragmentManager();

        SettingsComponentFragment componentFragment = (SettingsComponentFragment)
                fm.findFragmentByTag(SettingsComponentFragment.FRAGMENT_TAG);
        if (componentFragment == null) {
            componentFragment = SettingsComponentFragment.newInstance();
            fm.beginTransaction()
                    .add(componentFragment, SettingsComponentFragment.FRAGMENT_TAG)
                    .commitNow();
        }
        componentFragment.getComponent().inject(this);

        initializeUi();
    }

    private void initializeUi() {
        setContentView(R.layout.activity_settings);

        rootView = getWindow().getDecorView();

        toolbar.setLogo(R.drawable.ic_action_bar);
        toolbar.setTitle(R.string.activity_settings);

        final Settings settings = settingsPresenter.getSettings();
        rootView.setKeepScreenOn(settings.keepScreenOn());
        updateColor(settings);
        updateTextSize(settings);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsPresenter.login();
            }
        });
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO
            }
        });

        simpleReadingSwitch.setChecked(settings.isSimpleReading());
        simpleReadingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.setSimpleReading(isChecked);
            }
        });

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
                } catch (ActivityNotFoundException e) {
                    DialogHelper.showDialog(SettingsActivity.this, R.string.error_unknown_error, null);
                }
            }
        });

        inviteFriendsSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Resources resources = getResources();
                final Intent intent = new AppInviteInvitation.IntentBuilder(resources.getString(R.string.pref_invite_friends))
                        .setAndroidMinimumVersionCode(Build.VERSION_CODES.GINGERBREAD)
                        .setMessage(resources.getString(R.string.text_invite_friends_message))
                        .build();
                try {
                    startActivityForResult(intent, REQUEST_CODE_INVITE_FRIENDS);
                } catch (ActivityNotFoundException e) {
                    DialogHelper.showDialog(SettingsActivity.this, R.string.error_unknown_error, null);
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
                startActivity(OpenSourceLicenseActivity.newStartIntent(SettingsActivity.this));
            }
        });
    }

    private void animateColor(final int fromBackgroundColor, final int toBackgroundColor,
                              final int fromTitleTextColor, final int toTitleTextColor) {
        final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
        final ValueAnimator colorAnimator = ValueAnimator.ofFloat(0.0F, 1.0F);
        colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                final float fraction = (Float) animator.getAnimatedValue();
                final int backgroundColor = (Integer) argbEvaluator.evaluate(fraction, fromBackgroundColor, toBackgroundColor);
                final int titleTextColor = (Integer) argbEvaluator.evaluate(fraction, fromTitleTextColor, toTitleTextColor);
                updateColor(backgroundColor, titleTextColor);
            }
        });
        colorAnimator.setDuration(ANIMATION_DURATION).start();
    }

    private void updateColor(Settings settings) {
        updateColor(settings.getBackgroundColor(), settings.getTextColor());
    }

    private void updateColor(int backgroundColor, int titleTextColor) {
        rootView.setBackgroundColor(backgroundColor);

        loginButton.setTitleTextColor(titleTextColor);
        accountButton.setTitleTextColor(titleTextColor);
        logoutButton.setTitleTextColor(titleTextColor);
        simpleReadingSwitch.setTitleTextColor(titleTextColor);
        screenOnSwitch.setTitleTextColor(titleTextColor);
        nightModeSwitch.setTitleTextColor(titleTextColor);
        textSizeSettingButton.setTitleTextColor(titleTextColor);
        rateMeSettingButton.setTitleTextColor(titleTextColor);
        inviteFriendsSettingButton.setTitleTextColor(titleTextColor);
        versionSettingButton.setTitleTextColor(titleTextColor);
        licenseSettingButton.setTitleTextColor(titleTextColor);
    }

    private void animateTextSize(final float fromTextSize, final float toTextSize,
                                 final float fromSmallerTextSize, final float toSmallerTextSize) {
        final ValueAnimator textSizeAnimator = ValueAnimator.ofFloat(0.0F, 1.0F);
        textSizeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                final float fraction = (Float) animator.getAnimatedValue();
                final float textSize = fromTextSize + fraction * (toTextSize - fromTextSize);
                final float smallerTextSize = fromSmallerTextSize + fraction * (toSmallerTextSize - fromSmallerTextSize);
                updateTextSize(textSize, smallerTextSize);
            }
        });
        textSizeAnimator.setDuration(ANIMATION_DURATION).start();
    }

    private void updateTextSize(Settings settings) {
        final Settings.TextSize textSizeSetting = settings.getTextSize();
        final Resources resources = getResources();
        updateTextSize(resources.getDimension(textSizeSetting.textSize),
                resources.getDimension(textSizeSetting.smallerTextSize));
    }

    private void updateTextSize(float textSize, float smallerTextSize) {
        accountSectionHeader.setHeaderTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
        loginButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);
        accountButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);
        logoutButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);

        readingSectionHeader.setHeaderTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
        simpleReadingSwitch.setTitleTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);

        displaySectionHeader.setHeaderTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
        screenOnSwitch.setTitleTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        nightModeSwitch.setTitleTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        textSizeSettingButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);

        rateMeSettingButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);
        inviteFriendsSettingButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);
        aboutSectionHeader.setHeaderTextSize(TypedValue.COMPLEX_UNIT_PX, smallerTextSize);
        versionSettingButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);
        licenseSettingButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize, smallerTextSize);
    }

    @Override
    protected void onStart() {
        super.onStart();
        settingsPresenter.takeView(this);
    }

    @Override
    protected void onStop() {
        settingsPresenter.dropView();
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_INVITE_FRIENDS) {
            // do nothing
        } else if (requestCode == REQUEST_CODE_LOGIN) {
            settingsPresenter.handleLoginActivityResult(data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onUserLoggedIn(@NonNull User user) {
        loginButton.setVisibility(View.GONE);
        accountButton.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.VISIBLE);

        accountButton.setDescriptionText(user.displayName);
    }

    @Override
    public void onUserLoggedOut() {
        loginButton.setVisibility(View.VISIBLE);
        accountButton.setVisibility(View.GONE);
        logoutButton.setVisibility(View.GONE);
    }

    @Override
    public void onStartLoginActivity(@NonNull Intent intent) {
        startActivityForResult(intent, REQUEST_CODE_LOGIN);
    }
}
