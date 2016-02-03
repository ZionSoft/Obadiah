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

package net.zionsoft.obadiah.ui.utils;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import net.zionsoft.obadiah.R;

import butterknife.Bind;

public abstract class BaseRecyclerViewActivity extends BaseAppCompatActivity
        implements RecyclerView.OnChildAttachStateChangeListener, View.OnClickListener {
    @Bind(R.id.toolbar)
    protected Toolbar toolbar;

    @Bind(R.id.recycler_view)
    protected RecyclerView recyclerView;

    @Bind(R.id.loading_spinner)
    protected View loadingSpinner;

    @CallSuper
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_recycler_view);
        toolbar.setLogo(R.drawable.ic_action_bar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.addOnChildAttachStateChangeListener(this);
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
        view.setOnClickListener(this);
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        view.setOnClickListener(null);
    }

    @Override
    public void onClick(View v) {
        final int position = recyclerView.getChildAdapterPosition(v);
        if (position != RecyclerView.NO_POSITION) {
            onChildClicked(position);
        }
    }

    abstract protected void onChildClicked(int position);
}
