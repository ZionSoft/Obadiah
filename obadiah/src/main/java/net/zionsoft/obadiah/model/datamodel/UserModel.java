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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import net.zionsoft.obadiah.model.domain.User;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

@Singleton
public class UserModel implements FirebaseAuth.AuthStateListener {
    private final FirebaseAuth firebaseAuth;

    private final SerializedSubject<User, User> currentUserUpdatesSubject
            = PublishSubject.<User>create().toSerialized();

    @Inject
    public UserModel() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.addAuthStateListener(this);
    }

    @Nullable
    public User getCurrentUser() {
        final FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            return null;
        }
        return new User(firebaseUser.getUid(), firebaseUser.getDisplayName());
    }

    @NonNull
    public Observable<User> observeCurrentUser() {
        return currentUserUpdatesSubject.asObservable();
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
        currentUserUpdatesSubject.onNext(getCurrentUser());
    }
}
