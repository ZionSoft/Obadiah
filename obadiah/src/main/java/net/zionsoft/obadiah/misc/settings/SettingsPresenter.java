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
import com.google.android.gms.common.api.Status;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.datamodel.UserModel;
import net.zionsoft.obadiah.model.domain.User;
import net.zionsoft.obadiah.mvp.BasePresenter;

import java.util.concurrent.Callable;

import rx.Completable;
import rx.SingleSubscriber;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

class SettingsPresenter extends BasePresenter<SettingsView> {
    private final UserModel userModel;
    @SuppressWarnings("WeakerAccess")
    final GoogleApiClient googleApiClient;

    private Subscription currentUserSubscription;
    private Subscription loginSubscription;
    private Subscription logoutSubscription;

    SettingsPresenter(Context context, UserModel userModel, Settings settings) {
        super(settings);
        this.userModel = userModel;

        final GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.google_client_id))
                .requestEmail()
                .build();
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();
    }

    @Override
    protected void onViewTaken() {
        super.onViewTaken();

        googleApiClient.connect();

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
        unsubscribeLogoutSubscription();
        googleApiClient.disconnect();

        super.onViewDropped();
    }

    private void unsubscribeLoginSubscription() {
        if (loginSubscription != null) {
            loginSubscription.unsubscribe();
            loginSubscription = null;
        }
    }

    private void unsubscribeLogoutSubscription() {
        if (logoutSubscription != null) {
            logoutSubscription.unsubscribe();
            logoutSubscription = null;
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
            v.onUserLoginFailed();
        }
    }

    void handleLoginActivityResult(Intent data) {
        final SettingsView v = getView();
        if (v == null) {
            return;
        }

        final GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
        if (result.isSuccess()) {
            final GoogleSignInAccount account = result.getSignInAccount();
            if (account != null) {
                unsubscribeLoginSubscription();
                unsubscribeLogoutSubscription();

                loginSubscription = userModel.login(account.getIdToken())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleSubscriber<User>() {
                            @Override
                            public void onSuccess(User user) {
                                // do nothing
                            }

                            @Override
                            public void onError(Throwable e) {
                                final SettingsView v = getView();
                                if (v != null) {
                                    v.onUserLoginFailed();
                                }
                            }
                        });
                return;
            }
        }

        v.onUserLoginFailed();
    }

    void logout() {
        unsubscribeLoginSubscription();
        unsubscribeLogoutSubscription();

        logoutSubscription = userModel.logout()
                .andThen(Completable.fromCallable(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        final Status status = Auth.GoogleSignInApi.signOut(googleApiClient).await();
                        if (!status.isSuccess()) {
                            throw new RuntimeException("Failed to logout from Google API client");
                        }
                        return null;
                    }
                })).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action0() {
                    @Override
                    public void call() {
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable e) {
                        final SettingsView v = getView();
                        if (v != null) {
                            v.onUserLogoutFailed();
                        }
                    }
                });
    }
}
