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

package net.zionsoft.obadiah.biblereading.verse;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.ui.widget.Switch;

import butterknife.BindView;
import butterknife.ButterKnife;

public class VerseDetailDialogFragment extends DialogFragment implements Dialog.OnClickListener {
    private static final String KEY_TITLE = "net.zionsoft.obadiah.KEY_TITLE";
    private static final String KEY_BOOKMARKED = "net.zionsoft.obadiah.KEY_BOOKMARKED";
    private static final String KEY_NOTE = "net.zionsoft.obadiah.KEY_NOTE";

    @NonNull
    static VerseDetailDialogFragment newInstance(String title, boolean bookmarked, String note) {
        final VerseDetailDialogFragment fragment = new VerseDetailDialogFragment();

        final Bundle args = new Bundle();
        args.putString(KEY_TITLE, title);
        args.putBoolean(KEY_BOOKMARKED, bookmarked);
        if (!TextUtils.isEmpty(note)) {
            args.putString(KEY_NOTE, note);
        }
        fragment.setArguments(args);

        return fragment;
    }

    @BindView(R.id.bookmark)
    Switch bookmark;

    @BindView(R.id.note)
    TextInputEditText note;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity)
                .setTitle(getArguments().getString(KEY_TITLE))
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this);

        final View view = onCreateView(activity.getLayoutInflater(), null, savedInstanceState);
        onViewCreated(view, savedInstanceState);
        dialogBuilder.setView(view);

        return dialogBuilder.create();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_verse_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        final Bundle args = getArguments();
        bookmark.setChecked(args.getBoolean(KEY_BOOKMARKED));
        note.setText(args.getString(KEY_NOTE));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                // TODO
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                dismiss();
                break;
        }
    }
}
