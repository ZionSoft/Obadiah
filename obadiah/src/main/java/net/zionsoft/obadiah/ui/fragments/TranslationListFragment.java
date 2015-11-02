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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.injection.components.TranslationManagementComponent;
import net.zionsoft.obadiah.injection.scopes.ActivityScope;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.translations.TranslationInfo;
import net.zionsoft.obadiah.model.translations.TranslationManager;
import net.zionsoft.obadiah.mvp.presenters.TranslationManagementPresenter;
import net.zionsoft.obadiah.mvp.views.TranslationManagementView;
import net.zionsoft.obadiah.ui.adapters.TranslationListAdapter;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;

public class TranslationListFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener,
        TranslationManager.OnTranslationsLoadedListener, TranslationManagementView {
    private static final String TAG_DOWNLOAD_DIALOG_FRAGMENT = "net.zionsoft.obadiah.ui.fragments.TranslationListFragment.TAG_DOWNLOAD_DIALOG_FRAGMENT";
    private static final String TAG_REMOVE_DIALOG_FRAGMENT = "net.zionsoft.obadiah.ui.fragments.TranslationListFragment.TAG_REMOVE_DIALOG_FRAGMENT";
    private static final int CONTEXT_MENU_ITEM_DELETE = 0;

    @ActivityScope
    @Inject
    TranslationManagementPresenter translationManagementPresenter;

    @Inject
    TranslationManager translationManager;

    @Bind(R.id.swipe_container)
    SwipeRefreshLayout swipeContainer;

    @Bind(R.id.translation_list_view)
    ListView translationListView;

    private SharedPreferences preferences;
    private String currentTranslation;

    private TranslationListAdapter translationListAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        setRetainInstance(true);
        preferences = activity.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        currentTranslation = preferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadTranslations(true);
    }

    private void initializeUi() {
        swipeContainer.setColorSchemeResources(R.color.dark_cyan, R.color.dark_lime, R.color.blue, R.color.dark_blue);
        swipeContainer.setOnRefreshListener(this);

        // workaround for https://code.google.com/p/android/issues/detail?id=77712
        swipeContainer.setProgressViewOffset(false, 0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));
        swipeContainer.setRefreshing(true);

        translationListAdapter = new TranslationListAdapter(getActivity(), currentTranslation);
        translationListView.setAdapter(translationListAdapter);
        translationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!isAdded()) {
                    return;
                }

                final Pair<TranslationInfo, Boolean> translation
                        = translationListAdapter.getTranslation(position);
                if (translation == null) {
                    return;
                }

                if (translation.second) {
                    Analytics.trackTranslationSelection(translation.first.shortName);

                    preferences.edit()
                            .putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, translation.first.shortName)
                            .apply();

                    Activity activity = getActivity();
                    activity.finish();
                    activity.overridePendingTransition(R.anim.fade_in, R.anim.slide_out_left_to_right);
                } else {
                    downloadTranslation(translation.first);
                }
            }
        });
        registerForContextMenu(translationListView);
    }

    private void loadTranslations(final boolean forceRefresh) {
        translationListView.setVisibility(View.GONE);
        translationManager.loadTranslations(forceRefresh, this);
    }

    private void downloadTranslation(final TranslationInfo translationInfo) {
        final FragmentManager fm = getChildFragmentManager();
        ProgressDialogFragment.newInstance(R.string.progress_dialog_translation_downloading, 100).show(fm, TAG_DOWNLOAD_DIALOG_FRAGMENT);
        fm.executePendingTransactions();

        translationManager.downloadTranslation(translationInfo, new TranslationManager.OnTranslationDownloadListener() {
            @Override
            public void onTranslationDownloaded(String translation, boolean isSuccessful) {
                if (!isAdded())
                    return;

                final DialogFragment dialogFragment = (DialogFragment) getChildFragmentManager()
                        .findFragmentByTag(TAG_DOWNLOAD_DIALOG_FRAGMENT);
                if (dialogFragment != null) {
                    dialogFragment.dismissAllowingStateLoss();
                }

                if (isSuccessful) {
                    Toast.makeText(getActivity(), R.string.toast_translation_downloaded, Toast.LENGTH_SHORT).show();
                    if (currentTranslation == null) {
                        Analytics.trackTranslationSelection(translation);

                        currentTranslation = translation;
                        preferences.edit()
                                .putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, currentTranslation)
                                .apply();
                    }
                    loadTranslations(false);
                } else {
                    DialogHelper.showDialog(getActivity(), true, R.string.dialog_retry_network,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    downloadTranslation(translationInfo);
                                }
                            }, null);
                }
            }

            @Override
            public void onTranslationDownloadProgress(String translation, int progress) {
                if (!isAdded())
                    return;

                final DialogFragment dialogFragment = (DialogFragment) getChildFragmentManager()
                        .findFragmentByTag(TAG_DOWNLOAD_DIALOG_FRAGMENT);
                if (dialogFragment != null) {
                    ((ProgressDialog) dialogFragment.getDialog()).setProgress(progress);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        translationManagementPresenter.takeView(this);
    }

    @Override
    public void onPause() {
        translationManagementPresenter.dropView();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        unregisterForContextMenu(translationListView);

        super.onDestroyView();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v != translationListView) {
            super.onCreateContextMenu(menu, v, menuInfo);
            return;
        }

        final TranslationInfo translationInfo
                = getTranslationInfo((AdapterView.AdapterContextMenuInfo) menuInfo);
        if (translationInfo == null || currentTranslation.equals(translationInfo.name)) {
            return;
        }

        menu.setHeaderTitle(translationInfo.name);
        menu.add(Menu.NONE, CONTEXT_MENU_ITEM_DELETE, Menu.NONE, R.string.action_delete_translation);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo contextMenuInfo
                = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (contextMenuInfo == null)
            return super.onContextItemSelected(item);

        final TranslationInfo translationInfo = getTranslationInfo(contextMenuInfo);
        if (translationInfo == null) {
            return super.onContextItemSelected(item);
        }

        switch (item.getItemId()) {
            case CONTEXT_MENU_ITEM_DELETE:
                DialogHelper.showDialog(getActivity(), true, R.string.dialog_translation_delete_confirm_message,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                removeTranslation(translationInfo);
                            }
                        }, null
                );
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Nullable
    private TranslationInfo getTranslationInfo(AdapterView.AdapterContextMenuInfo contextMenuInfo) {
        final Pair<TranslationInfo, Boolean> translation
                = translationListAdapter.getTranslation(contextMenuInfo.position);
        return translation != null && translation.second ? translation.first : null;
    }

    private void removeTranslation(TranslationInfo translation) {
        final FragmentManager fm = getChildFragmentManager();
        ProgressDialogFragment.newInstance(R.string.progress_dialog_translation_deleting).show(fm, TAG_REMOVE_DIALOG_FRAGMENT);
        fm.executePendingTransactions();

        translationManagementPresenter.removeTranslation(translation);
    }

    @Override
    public void onRefresh() {
        loadTranslations(true);
    }

    @Override
    public void onTranslationRemoved() {
        ((DialogFragment) getChildFragmentManager().findFragmentByTag(TAG_REMOVE_DIALOG_FRAGMENT))
                .dismissAllowingStateLoss();

        Toast.makeText(getActivity(), R.string.toast_translation_deleted, Toast.LENGTH_SHORT).show();
        loadTranslations(false);
    }

    @Override
    public void onTranslationRemovalFailed(final TranslationInfo translation) {
        ((DialogFragment) getChildFragmentManager().findFragmentByTag(TAG_REMOVE_DIALOG_FRAGMENT))
                .dismissAllowingStateLoss();

        DialogHelper.showDialog(getActivity(), true, R.string.dialog_translation_remove_failure_message,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        removeTranslation(translation);
                    }
                }, null);
    }

    @Override
    public void onTranslationsLoaded(@Nullable List<TranslationInfo> downloaded, @Nullable List<TranslationInfo> available) {
        if (!isAdded()) {
            return;
        }

        if (downloaded == null || available == null) {
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
            return;
        }

        swipeContainer.setRefreshing(false);
        AnimationHelper.fadeIn(translationListView);

        translationListAdapter.setTranslations(downloaded, available);
        translationListAdapter.notifyDataSetChanged();
    }
}
