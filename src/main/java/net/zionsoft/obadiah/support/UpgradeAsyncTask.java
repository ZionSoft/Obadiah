/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2013 ZionSoft
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

package net.zionsoft.obadiah.support;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import net.zionsoft.obadiah.BookSelectionActivity;
import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;

public class UpgradeAsyncTask extends AsyncTask<Void, Integer, Void> {
    public UpgradeAsyncTask(BookSelectionActivity bookSelectionActivity) {
        super();
        mBookSelectionActivity = bookSelectionActivity;
    }

    protected void onPreExecute() {
        // running in the main thread

        mProgressDialog = new ProgressDialog(mBookSelectionActivity);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(mBookSelectionActivity.getText(R.string.text_initializing));
        mProgressDialog.show();
    }

    protected Void doInBackground(Void... params) {
        // running in the worker thread

        final SharedPreferences preferences
                = mBookSelectionActivity.getSharedPreferences(Constants.PREF_NAME,
                Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();

        final int version = preferences.getInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION, 0);
        if (version < 10500) {
            // prior to 1.5.0

            // upgrading from prior to 1.5.0 is no longer supported since 1.7.0
            // now simply delete all the old data
            Utils.removeDirectory(mBookSelectionActivity.getFilesDir());
            editor.remove("selectedTranslation");
        }

        // sets the application version
        editor.putInt(Constants.PREF_KEY_CURRENT_APPLICATION_VERSION,
                Constants.CURRENT_APPLICATION_VERSION);
        editor.commit();

        return null;
    }

    protected void onPostExecute(Void result) {
        // running in the main thread

        mBookSelectionActivity.onUpgradeFinished();
        mProgressDialog.dismiss();
    }

    private BookSelectionActivity mBookSelectionActivity;
    private ProgressDialog mProgressDialog;
}
