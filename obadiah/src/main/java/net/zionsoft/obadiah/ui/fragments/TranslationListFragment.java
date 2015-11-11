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

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.injection.components.TranslationManagementComponent;
import net.zionsoft.obadiah.injection.scopes.ActivityScope;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.translations.TranslationInfo;
import net.zionsoft.obadiah.model.translations.Translations;
import net.zionsoft.obadiah.mvp.presenters.TranslationManagementPresenter;
import net.zionsoft.obadiah.mvp.views.TranslationManagementView;
import net.zionsoft.obadiah.ui.adapters.TranslationListAdapter;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;
import net.zionsoft.obadiah.ui.widget.ProgressDialog;

import javax.inject.Inject;

import butterknife.Bind;

public class TranslationListFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
        TranslationManagementView, RecyclerView.OnChildAttachStateChangeListener, View.OnClickListener,
        View.OnCreateContextMenuListener {
    private static final int CONTEXT_MENU_ITEM_DELETE = 0;

    @ActivityScope
    @Inject
    TranslationManagementPresenter translationManagementPresenter;

    @Inject
    Settings settings;

    @Bind(R.id.swipe_container)
    SwipeRefreshLayout swipeContainer;

    @Bind(R.id.translation_list)
    RecyclerView translationList;

    private TranslationListAdapter translationListAdapter;

    private ProgressDialog removeTranslationProgressDialog;
    private ProgressDialog downloadTranslationProgressDialog;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TranslationManagementComponent.Initializer.init(App.get(getActivity()).getInjectionComponent())
                .inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_translation_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeUi();
    }

    private void initializeUi() {
        swipeContainer.setColorSchemeResources(R.color.dark_cyan, R.color.dark_lime, R.color.blue, R.color.dark_blue);
        swipeContainer.setOnRefreshListener(this);

        // workaround for https://code.google.com/p/android/issues/detail?id=77712
        swipeContainer.setProgressViewOffset(false, 0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));
        swipeContainer.setRefreshing(true);

        translationList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        translationList.addOnChildAttachStateChangeListener(this);

        translationListAdapter = new TranslationListAdapter(getActivity(), settings,
                translationManagementPresenter.loadCurrentTranslation());
        translationList.setAdapter(translationListAdapter);
    }

    private void loadTranslations(final boolean forceRefresh) {
        translationList.setVisibility(View.GONE);
        translationManagementPresenter.loadTranslations(forceRefresh);
    }

    private void downloadTranslation(TranslationInfo translationInfo) {
        getOrCreateDownloadDialog();
        translationManagementPresenter.fetchTranslation(translationInfo);
    }

    @NonNull
    private ProgressDialog getOrCreateDownloadDialog() {
        if (downloadTranslationProgressDialog == null) {
            downloadTranslationProgressDialog = ProgressDialog.showProgressDialog(
                    getActivity(), R.string.progress_dialog_translation_downloading, 100);
        }
        return downloadTranslationProgressDialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        translationManagementPresenter.takeView(this);
        loadTranslations(true);
    }

    @Override
    public void onPause() {
        translationManagementPresenter.dropView();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        dismissRemoveProgressDialog();
        dismissDownloadProgressDialog();

        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        translationManagementPresenter.cancelRemoveTranslation();
        translationManagementPresenter.cancelFetchTranslation();
        super.onDestroy();
    }

    @Override
    public void onRefresh() {
        loadTranslations(true);
    }

    @Override
    public void onTranslationLoaded(Translations translations) {
        swipeContainer.setRefreshing(false);
        AnimationHelper.fadeIn(translationList);

        translationListAdapter.setTranslations(translations);
        translationListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onTranslationLoadFailed() {
        DialogHelper.showDialog(getActivity(), false, R.string.dialog_retry_network,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadTranslations(true);
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Activity activity = getActivity();
                        activity.finish();
                        activity.overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left_to_right);
                    }
                }
        );
    }

    @Override
    public void onTranslationRemoved(TranslationInfo translation) {
        dismissRemoveProgressDialog();
        Toast.makeText(getActivity(), R.string.toast_translation_deleted, Toast.LENGTH_SHORT).show();
        loadTranslations(false);
    }

    private void dismissRemoveProgressDialog() {
        if (removeTranslationProgressDialog != null) {
            removeTranslationProgressDialog.dismiss();
            removeTranslationProgressDialog = null;
        }
    }

    @Override
    public void onTranslationRemovalFailed(final TranslationInfo translation) {
        dismissRemoveProgressDialog();

        DialogHelper.showDialog(getActivity(), true, R.string.dialog_translation_remove_failure_message,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        removeTranslation(translation);
                    }
                }, null);
    }

    @Override
    public void onTranslationDownloadProgressed(TranslationInfo translation, int progress) {
        getOrCreateDownloadDialog().setProgress(progress);
    }

    @Override
    public void onTranslationDownloaded(TranslationInfo translation) {
        dismissDownloadProgressDialog();

        Toast.makeText(getActivity(), R.string.toast_translation_downloaded, Toast.LENGTH_SHORT).show();
        loadTranslations(false);
    }

    private void dismissDownloadProgressDialog() {
        if (downloadTranslationProgressDialog != null) {
            downloadTranslationProgressDialog.dismiss();
            downloadTranslationProgressDialog = null;
        }
    }

    @Override
    public void onTranslationDownloadFailed(final TranslationInfo translation) {
        dismissDownloadProgressDialog();

        DialogHelper.showDialog(getActivity(), true, R.string.dialog_retry_network,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        downloadTranslation(translation);
                    }
                }, null);
    }

    @Override
    public void onChildViewAttachedToWindow(View view) {
        view.setOnClickListener(this);
        view.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onChildViewDetachedFromWindow(View view) {
        view.setOnClickListener(null);
        view.setOnCreateContextMenuListener(null);
    }

    @Override
    public void onClick(View v) {
        final TranslationListAdapter.TranslationViewHolder translationViewHolder = getTranslationViewHolder(v);
        if (translationViewHolder == null) {
            return;
        }
        if (translationViewHolder.isDownloaded()) {
            translationManagementPresenter.saveCurrentTranslation(translationViewHolder.getTranslationInfo());

            Activity activity = getActivity();
            activity.finish();
            activity.overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left_to_right);
        } else {
            downloadTranslation(translationViewHolder.getTranslationInfo());
        }
    }

    @Nullable
    private TranslationListAdapter.TranslationViewHolder getTranslationViewHolder(View v) {
        final int position = translationList.getChildAdapterPosition(v);
        if (position == RecyclerView.NO_POSITION) {
            return null;
        }
        final RecyclerView.ViewHolder viewHolder = translationList.findViewHolderForAdapterPosition(position);
        if (!(viewHolder instanceof TranslationListAdapter.TranslationViewHolder)) {
            return null;
        }
        return (TranslationListAdapter.TranslationViewHolder) viewHolder;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        final TranslationListAdapter.TranslationViewHolder translationViewHolder = getTranslationViewHolder(v);
        if (translationViewHolder == null) {
            return;
        }
        if (translationViewHolder.isDownloaded()) {
            menu.setHeaderTitle(translationViewHolder.getTranslationInfo().name);
            menu.add(Menu.NONE, CONTEXT_MENU_ITEM_DELETE, Menu.NONE, R.string.action_delete_translation)
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case CONTEXT_MENU_ITEM_DELETE:
                                    DialogHelper.showDialog(getActivity(), true,
                                            R.string.dialog_translation_delete_confirm_message,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    removeTranslation(translationViewHolder.getTranslationInfo());
                                                }
                                            }, null);
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
        }
    }

    private void removeTranslation(TranslationInfo translation) {
        removeTranslationProgressDialog = ProgressDialog.showIndeterminateProgressDialog(
                getActivity(), R.string.progress_dialog_translation_deleting);

        translationManagementPresenter.removeTranslation(translation);
    }
}
