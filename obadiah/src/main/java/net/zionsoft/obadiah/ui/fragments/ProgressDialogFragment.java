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

package net.zionsoft.obadiah.ui.fragments;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

public class ProgressDialogFragment extends DialogFragment {
    private static final String KEY_MAX_PROGRESS = "net.zionsoft.obadiah.ui.fragments.ProgressDialogFragment.KEY_MAX_PROGRESS";
    private static final String KEY_MESSAGE = "net.zionsoft.obadiah.ui.fragments.ProgressDialogFragment.KEY_MESSAGE";

    public static ProgressDialogFragment newInstance(int message) {
        final ProgressDialogFragment fragment = new ProgressDialogFragment();
        fragment.setCancelable(false);

        final Bundle args = new Bundle();
        args.putInt(KEY_MESSAGE, message);
        fragment.setArguments(args);

        return fragment;
    }

    public static ProgressDialogFragment newInstance(int message, int maxProgress) {
        final ProgressDialogFragment fragment = new ProgressDialogFragment();
        fragment.setCancelable(false);

        final Bundle args = new Bundle();
        args.putInt(KEY_MAX_PROGRESS, maxProgress);
        args.putInt(KEY_MESSAGE, message);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ProgressDialog dialog = new ProgressDialog(getActivity());

        final Bundle args = getArguments();
        if (args.containsKey(KEY_MAX_PROGRESS)) {
            dialog.setIndeterminate(false);
            dialog.setMax(args.getInt(KEY_MAX_PROGRESS));
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        }
        dialog.setMessage(getString(args.getInt(KEY_MESSAGE)));

        dialog.show();
        return dialog;
    }
}
