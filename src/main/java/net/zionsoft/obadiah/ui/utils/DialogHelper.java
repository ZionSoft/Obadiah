/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2014 ZionSoft
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

package net.zionsoft.obadiah.ui.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;

public class DialogHelper {
    public static void showDialog(Context context, @StringRes int message,
                                  DialogInterface.OnClickListener onClicked) {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, onClicked)
                .setMessage(message)
                .create().show();
    }

    public static void showDialog(Context context, boolean cancelable, int message,
                                  DialogInterface.OnClickListener onPositive,
                                  DialogInterface.OnClickListener onNegative) {
        new AlertDialog.Builder(context)
                .setCancelable(cancelable)
                .setPositiveButton(android.R.string.yes, onPositive)
                .setNegativeButton(android.R.string.no, onNegative)
                .setMessage(message)
                .create().show();
    }
}
