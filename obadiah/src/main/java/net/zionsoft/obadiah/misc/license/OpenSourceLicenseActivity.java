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

package net.zionsoft.obadiah.misc.license;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.ui.utils.BaseRecyclerViewActivity;

import java.util.List;

import javax.inject.Inject;

public class OpenSourceLicenseActivity extends BaseRecyclerViewActivity implements OpenSourceLicenseView {
    public static Intent newStartIntent(Context context) {
        return new Intent(context, OpenSourceLicenseActivity.class);
    }

    @Inject
    OpenSourceLicensePresenter openSourceLicensePresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getSupportFragmentManager();
        OpenSourceLicenseComponentFragment componentFragment = (OpenSourceLicenseComponentFragment)
                fm.findFragmentByTag(OpenSourceLicenseComponentFragment.FRAGMENT_TAG);
        if (componentFragment == null) {
            componentFragment = OpenSourceLicenseComponentFragment.newInstance();
            fm.beginTransaction()
                    .add(componentFragment, OpenSourceLicenseComponentFragment.FRAGMENT_TAG)
                    .commitNow();
        }
        componentFragment.getComponent().inject(this);

        final View rootView = getWindow().getDecorView();
        final Settings settings = openSourceLicensePresenter.getSettings();
        rootView.setKeepScreenOn(settings.keepScreenOn());
        rootView.setBackgroundColor(settings.getBackgroundColor());

        toolbar.setTitle(R.string.activity_open_source_license);
    }

    @Override
    protected void onStart() {
        super.onStart();
        openSourceLicensePresenter.takeView(this);
        openSourceLicensePresenter.loadLicense();
    }

    @Override
    protected void onStop() {
        openSourceLicensePresenter.dropView();
        super.onStop();
    }

    @Override
    public void onLicensesLoaded(List<String> licenses) {
        recyclerView.setAdapter(new OpenSourceLicenseListAdapter(
                this, openSourceLicensePresenter.getSettings(), licenses));
        loadingSpinner.setVisibility(View.GONE);
    }

    @Override
    protected void onChildClicked(int position) {
        // do nothing
    }
}
