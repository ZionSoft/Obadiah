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

package net.zionsoft.obadiah.ui.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.TranslationInfo;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.ui.adapters.TranslationListAdapter;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

import javax.inject.Inject;

public class TranslationListFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG_DOWNLOAD_DIALOG_FRAGMENT = "net.zionsoft.obadiah.ui.fragments.TranslationListFragment.TAG_DOWNLOAD_DIALOG_FRAGMENT";
    private static final String TAG_REMOVE_DIALOG_FRAGMENT = "net.zionsoft.obadiah.ui.fragments.TranslationListFragment.TAG_REMOVE_DIALOG_FRAGMENT";
    private static final int CONTEXT_MENU_ITEM_DELETE = 0;

    @Inject
    Bible mBible;

    private SharedPreferences mPreferences;
    private String mCurrentTranslation;

    private TranslationListAdapter mTranslationListAdapter;

    private SwipeRefreshLayout mSwipeContainer;
    private ListView mTranslationListView;

    public TranslationListFragment() {
        super();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        setRetainInstance(true);
        mPreferences = activity.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        mCurrentTranslation = mPreferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_translation_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSwipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
        mSwipeContainer.setColorSchemeResources(R.color.dark_cyan, R.color.dark_lime, R.color.blue, R.color.dark_blue);
        mSwipeContainer.setOnRefreshListener(this);

        // workaround for https://code.google.com/p/android/issues/detail?id=77712
        mSwipeContainer.setProgressViewOffset(false, 0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));
        mSwipeContainer.setRefreshing(true);

        mTranslationListView = (ListView) view.findViewById(R.id.translation_list_view);
        mTranslationListAdapter = new TranslationListAdapter(getActivity(), mCurrentTranslation);
        mTranslationListView.setAdapter(mTranslationListAdapter);
        mTranslationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!isAdded()) {
                    return;
                }

                final Pair<TranslationInfo, Boolean> translation
                        = mTranslationListAdapter.getTranslation(position);
                if (translation == null) {
                    return;
                }

                if (translation.second) {
                    Analytics.trackTranslationSelection(translation.first.shortName);

                    mPreferences.edit()
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
        registerForContextMenu(mTranslationListView);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        App.get(getActivity()).getInjectionComponent().inject(this);

        loadTranslations(true);
    }

    private void loadTranslations(final boolean forceRefresh) {
        mTranslationListView.setVisibility(View.GONE);

        mBible.loadTranslations(forceRefresh, new Bible.OnTranslationsLoadedListener() {
            @Override
            public void onTranslationsLoaded(List<TranslationInfo> downloaded, List<TranslationInfo> available) {
                if (!isAdded())
                    return;

                if (downloaded == null || available == null) {
                    DialogHelper.showDialog(getActivity(), false, R.string.dialog_retry_network,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    loadTranslations(forceRefresh);
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

                mSwipeContainer.setRefreshing(false);
                AnimationHelper.fadeIn(mTranslationListView);

                mTranslationListAdapter.setTranslations(downloaded, available);
                mTranslationListAdapter.notifyDataSetChanged();
            }
        });
    }

    private void downloadTranslation(final TranslationInfo translationInfo) {
        final FragmentManager fm = getChildFragmentManager();
        ProgressDialogFragment.newInstance(R.string.progress_dialog_translation_downloading, 100).show(fm, TAG_DOWNLOAD_DIALOG_FRAGMENT);
        fm.executePendingTransactions();

        mBible.downloadTranslation(translationInfo, new Bible.OnTranslationDownloadListener() {
            @Override
            public void onTranslationDownloaded(String translation, boolean isSuccessful) {
                if (!isAdded())
                    return;

                ((DialogFragment) getChildFragmentManager().findFragmentByTag(TAG_DOWNLOAD_DIALOG_FRAGMENT))
                        .dismissAllowingStateLoss();

                if (isSuccessful) {
                    Toast.makeText(getActivity(), R.string.toast_translation_downloaded, Toast.LENGTH_SHORT).show();
                    if (mCurrentTranslation == null) {
                        Analytics.trackTranslationSelection(translation);

                        mCurrentTranslation = translation;
                        mPreferences.edit()
                                .putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, mCurrentTranslation)
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

                final DialogFragment fragment = (DialogFragment) getChildFragmentManager()
                        .findFragmentByTag(TAG_DOWNLOAD_DIALOG_FRAGMENT);
                ((ProgressDialog) fragment.getDialog()).setProgress(progress);
            }
        });
    }

    @Override
    public void onDestroyView() {
        unregisterForContextMenu(mTranslationListView);

        super.onDestroyView();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v != mTranslationListView) {
            super.onCreateContextMenu(menu, v, menuInfo);
            return;
        }

        final TranslationInfo translationInfo
                = getTranslationInfo((AdapterView.AdapterContextMenuInfo) menuInfo);
        if (translationInfo == null || mCurrentTranslation.equals(translationInfo.name)) {
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
                                removeTranslation(translationInfo.shortName);
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
                = mTranslationListAdapter.getTranslation(contextMenuInfo.position);
        return translation != null && translation.second ? translation.first : null;
    }

    private void removeTranslation(String translationShortName) {
        final FragmentManager fm = getChildFragmentManager();
        ProgressDialogFragment.newInstance(R.string.progress_dialog_translation_deleting).show(fm, TAG_REMOVE_DIALOG_FRAGMENT);
        fm.executePendingTransactions();

        mBible.removeTranslation(translationShortName, new Bible.OnTranslationRemovedListener() {
            @Override
            public void onTranslationRemoved(final String translation, boolean isSuccessful) {
                if (!isAdded())
                    return;

                ((DialogFragment) getChildFragmentManager().findFragmentByTag(TAG_REMOVE_DIALOG_FRAGMENT))
                        .dismissAllowingStateLoss();

                if (isSuccessful) {
                    Toast.makeText(getActivity(), R.string.toast_translation_deleted, Toast.LENGTH_SHORT).show();
                    loadTranslations(false);
                } else {
                    DialogHelper.showDialog(getActivity(), true,
                            R.string.dialog_translation_remove_failure_message,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    removeTranslation(translation);
                                }
                            }, null
                    );
                }
            }
        });
    }

    @Override
    public void onRefresh() {
        loadTranslations(true);
    }
}
