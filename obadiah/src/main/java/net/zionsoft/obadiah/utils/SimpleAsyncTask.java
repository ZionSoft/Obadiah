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

package net.zionsoft.obadiah.utils;

import android.os.AsyncTask;
import android.os.Build;

import com.crashlytics.android.Crashlytics;

import java.util.concurrent.RejectedExecutionException;

public abstract class SimpleAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    public void start(Params... params) {
        if (getStatus() != Status.PENDING) {
            Crashlytics.logException(new IllegalStateException("Attempted to start an async task with state " + getStatus()));
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            try {
                executeOnExecutor(THREAD_POOL_EXECUTOR, params);
            } catch (RejectedExecutionException e) {
                // well, it just fails
                Crashlytics.logException(e);
            }
        } else {
            execute(params);
        }
    }
}
