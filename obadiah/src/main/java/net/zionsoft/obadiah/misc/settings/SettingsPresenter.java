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

import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.datamodel.UserModel;
import net.zionsoft.obadiah.model.domain.User;
import net.zionsoft.obadiah.mvp.BasePresenter;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

class SettingsPresenter extends BasePresenter<SettingsView> {
    private final UserModel userModel;

    private Subscription currentUserSubscription;

    SettingsPresenter(UserModel userModel, Settings settings) {
        super(settings);
        this.userModel = userModel;
    }

    @Override
    protected void onViewTaken() {
        super.onViewTaken();

        currentUserSubscription = userModel.observeCurrentUser()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<User>() {
                    @Override
                    public void onCompleted() {
                        // won't reach here
                    }

                    @Override
                    public void onError(Throwable e) {
                        // should I do anything?
                    }

                    @Override
                    public void onNext(User user) {
                        final SettingsView v = getView();
                        if (v != null) {
                            if (user != null) {
                                v.onUserLoggedIn(user);
                            } else {
                                v.onUserLoggedOut();
                            }
                        }
                    }
                });
    }

    @Override
    protected void onViewDropped() {
        if (currentUserSubscription != null) {
            currentUserSubscription.unsubscribe();
            currentUserSubscription = null;
        }

        super.onViewDropped();
    }
}
