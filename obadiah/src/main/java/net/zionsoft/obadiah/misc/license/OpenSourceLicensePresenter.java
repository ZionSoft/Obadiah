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

package net.zionsoft.obadiah.misc.license;

import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.mvp.BasePresenter;
import net.zionsoft.obadiah.utils.RxHelper;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;

class OpenSourceLicensePresenter extends BasePresenter<OpenSourceLicenseView> {
    private final OpenSourceLicenseModel openSourceLicenseModel;

    private Subscription subscription;

    OpenSourceLicensePresenter(OpenSourceLicenseModel openSourceLicenseModel, Settings settings) {
        super(settings);
        this.openSourceLicenseModel = openSourceLicenseModel;
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }

    void loadLicense() {
        subscription = openSourceLicenseModel.loadLicense()
                .compose(RxHelper.<List<String>>applySchedulers())
                .subscribe(new Action1<List<String>>() {
                    @Override
                    public void call(List<String> licenses) {
                        OpenSourceLicenseView v = getView();
                        if (v != null) {
                            v.onLicensesLoaded(licenses);
                        }
                    }
                });
    }
}
