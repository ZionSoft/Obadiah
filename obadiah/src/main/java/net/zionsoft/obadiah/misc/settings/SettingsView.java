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

package net.zionsoft.obadiah.misc.settings;

import android.content.Intent;
import android.support.annotation.NonNull;

import net.zionsoft.obadiah.model.domain.User;
import net.zionsoft.obadiah.mvp.MVPView;

interface SettingsView extends MVPView {
    void onUserLoggedIn(@NonNull User user);

    void onUserLoggedOut();

    void onStartLoginActivity(@NonNull Intent intent);

    void onUserLoginFailed();

    void onUserLogoutFailed();
}
