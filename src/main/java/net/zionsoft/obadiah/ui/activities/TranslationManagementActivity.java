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

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Analytics;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.ui.fragments.TranslationListFragment;

public class TranslationManagementActivity extends ActionBarActivity {
    private static final String TAG_TRANSLATION_LIST_FRAGMENT = "net.zionsoft.obadiah.ui.activities.TranslationManagementActivity.TAG_TRANSLATION_LIST_FRAGMENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_translation_management);
        getWindow().getDecorView().setBackgroundColor(Settings.getInstance().getBackgroundColor());

        final FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(TAG_TRANSLATION_LIST_FRAGMENT) == null) {
            // otherwise an extra fragment will be added in case of orientation change
            fm.beginTransaction()
                    .replace(R.id.container, TranslationListFragment.newInstance(), TAG_TRANSLATION_LIST_FRAGMENT)
                    .commit();
        }

        ((AdView) findViewById(R.id.ad_view)).loadAd(new AdRequest.Builder()
                .addKeyword("bible").addKeyword("jesus").addKeyword("christian")
                .build());
    }

    @Override
    protected void onResume() {
        super.onResume();

        Analytics.trackScreen(TranslationManagementActivity.class.getSimpleName());
    }
}
