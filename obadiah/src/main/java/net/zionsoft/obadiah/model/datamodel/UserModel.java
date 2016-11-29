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

package net.zionsoft.obadiah.model.datamodel;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import net.zionsoft.obadiah.model.domain.User;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

@Singleton
public class UserModel implements FirebaseAuth.AuthStateListener {
    @SuppressWarnings("WeakerAccess")
    final FirebaseAuth firebaseAuth;

    private final SerializedSubject<User, User> currentUserUpdatesSubject
            = PublishSubject.<User>create().toSerialized();

    @Inject
    public UserModel() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.addAuthStateListener(this);
    }

    @Nullable
    public User getCurrentUser() {
        return fromFirebaseUser(firebaseAuth.getCurrentUser());
    }

    @SuppressWarnings("WeakerAccess")
    @Nullable
    static User fromFirebaseUser(@Nullable FirebaseUser firebaseUser) {
        return firebaseUser != null ? new User(firebaseUser.getUid(), firebaseUser.getDisplayName()) : null;
    }

    @NonNull
    public Observable<User> observeCurrentUser() {
        return currentUserUpdatesSubject.asObservable();
    }

    @NonNull
    public Single<User> login(final String token) {
        return Single.create(new Single.OnSubscribe<User>() {
            @Override
            public void call(final SingleSubscriber<? super User> subscriber) {
                try {
                    final AuthCredential credential = GoogleAuthProvider.getCredential(token, null);
                    firebaseAuth.signInWithCredential(credential)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        subscriber.onSuccess(fromFirebaseUser(task.getResult().getUser()));
                                    } else {
                                        subscriber.onError(task.getException());
                                    }
                                }
                            });
                } catch (Throwable e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    @NonNull
    public Completable logout() {
        firebaseAuth.signOut();
        return Completable.complete();
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        currentUserUpdatesSubject.onNext(fromFirebaseUser(firebaseAuth.getCurrentUser()));
    }
}
