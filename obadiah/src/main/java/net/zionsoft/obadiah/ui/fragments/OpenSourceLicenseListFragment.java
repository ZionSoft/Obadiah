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

package net.zionsoft.obadiah.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;

import com.google.android.gms.common.GooglePlayServicesUtil;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.ui.adapters.OpenSourceLicenseListAdapter;
import net.zionsoft.obadiah.utils.SimpleAsyncTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OpenSourceLicenseListFragment extends ListFragment {
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setDivider(null);
        loadLicenses();
    }

    private void loadLicenses() {
        new SimpleAsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... params) {
                final List<String> licenses = new ArrayList<>();
                final Context context = getActivity();
                licenses.add(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(context));
                licenses.addAll(Arrays.asList(context.getResources().getStringArray(R.array.licenses)));
                return licenses;
            }

            @Override
            protected void onPostExecute(List<String> result) {
                if (!isAdded()) {
                    return;
                }

                if (result != null) {
                    setListAdapter(new OpenSourceLicenseListAdapter(getActivity(), result));
                } else {
                    // TODO
                }
            }
        }.start();
    }
}
