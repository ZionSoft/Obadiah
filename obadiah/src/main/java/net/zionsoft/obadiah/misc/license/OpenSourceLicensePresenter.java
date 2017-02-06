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

import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.mvp.BasePresenter;
import net.zionsoft.obadiah.utils.RxHelper;

import java.util.List;

import rx.SingleSubscriber;
import rx.Subscription;

class OpenSourceLicensePresenter extends BasePresenter<OpenSourceLicenseView> {
    private final OpenSourceLicenseModel openSourceLicenseModel;

    @SuppressWarnings("WeakerAccess")
    Subscription subscription;

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
                .compose(RxHelper.<List<String>>applySchedulersForSingle())
                .subscribe(new SingleSubscriber<List<String>>() {
                    @Override
                    public void onSuccess(List<String> licenses) {
                        subscription = null;
                        final OpenSourceLicenseView v = getView();
                        if (v != null) {
                            v.onLicensesLoaded(licenses);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        subscription = null;
                    }
                });
    }
}
