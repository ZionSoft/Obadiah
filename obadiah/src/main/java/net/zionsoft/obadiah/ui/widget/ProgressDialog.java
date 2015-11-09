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

package net.zionsoft.obadiah.ui.widget;

import android.content.Context;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;

import net.zionsoft.obadiah.R;

public class ProgressDialog {
    private final AlertDialog dialog;
    private final android.widget.ProgressBar progressBar;
    private final int maxProgress;

    private ProgressDialog(AlertDialog dialog, android.widget.ProgressBar progressBar, int maxProgress) {
        this.dialog = dialog;
        this.progressBar = progressBar;
        this.maxProgress = maxProgress;
    }

    public void dismiss() {
        dialog.dismiss();
    }

    public void setProgress(int progress) {
        if (progressBar != null && progress >= 0 && progress <= maxProgress) {
            progressBar.setProgress(progress);
        }
    }

    public static ProgressDialog showIndeterminateProgressDialog(Context context, @StringRes int title) {
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setView(View.inflate(context, R.layout.widget_indeterminate_progress_bar, null))
                .setTitle(title)
                .create();
        dialog.show();
        return new ProgressDialog(dialog, null, -1);
    }

    public static ProgressDialog showProgressDialog(Context context, @StringRes int title, int maxProgress) {
        final android.widget.ProgressBar progressBar = (android.widget.ProgressBar)
                View.inflate(context, R.layout.widget_progress_bar, null);
        progressBar.setMax(maxProgress);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setView(progressBar)
                .setTitle(title)
                .create();
        dialog.show();
        return new ProgressDialog(dialog, progressBar, maxProgress);
    }
}
