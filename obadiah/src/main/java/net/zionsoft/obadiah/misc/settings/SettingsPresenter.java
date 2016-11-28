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

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.datamodel.UserModel;
import net.zionsoft.obadiah.model.domain.User;
import net.zionsoft.obadiah.mvp.BasePresenter;

import rx.SingleSubscriber;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

class SettingsPresenter extends BasePresenter<SettingsView> {
    private final UserModel userModel;
    private final GoogleApiClient googleApiClient;

    private Subscription currentUserSubscription;
    private Subscription loginSubscription;

    SettingsPresenter(Context context, UserModel userModel, Settings settings) {
        super(settings);
        this.userModel = userModel;

        final GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.google_client_id))
                .requestEmail()
                .build();
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();
        googleApiClient.connect();
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
        unsubscribeLoginSubscription();

        super.onViewDropped();
    }

    private void unsubscribeLoginSubscription() {
        if (loginSubscription != null) {
            loginSubscription.unsubscribe();
            loginSubscription = null;
        }
    }

    void login() {
        final SettingsView v = getView();
        if (v == null) {
            return;
        }

        if (googleApiClient.isConnected()) {
            v.onStartLoginActivity(Auth.GoogleSignInApi.getSignInIntent(googleApiClient));
        } else {
            // TODO
        }
    }

    void handleLoginActivityResult(Intent data) {
        final GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
        if (result.isSuccess()) {
            final GoogleSignInAccount account = result.getSignInAccount();
            if (account != null) {
                unsubscribeLoginSubscription();
                loginSubscription = userModel.login(account.getIdToken())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleSubscriber<User>() {
                            @Override
                            public void onSuccess(User user) {
                                System.out.println("--> " + user.displayName + ", " + user.uid);
                            }

                            @Override
                            public void onError(Throwable e) {
                                e.printStackTrace();
                            }
                        });
            }
        }

        // TODO
    }
}
