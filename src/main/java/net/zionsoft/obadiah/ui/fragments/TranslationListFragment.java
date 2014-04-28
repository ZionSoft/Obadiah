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
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Analytics;
import net.zionsoft.obadiah.model.Bible;
import net.zionsoft.obadiah.model.TranslationInfo;
import net.zionsoft.obadiah.ui.adapters.TranslationExpandableListAdapter;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

public class TranslationListFragment extends Fragment {
    private static final String TAG_DOWNLOAD_DIALOG_FRAGMENT = "net.zionsoft.obadiah.ui.fragments.TranslationListFragment.TAG_DOWNLOAD_DIALOG_FRAGMENT";
    private static final String TAG_REMOVE_DIALOG_FRAGMENT = "net.zionsoft.obadiah.ui.fragments.TranslationListFragment.TAG_REMOVE_DIALOG_FRAGMENT";
    private static final int CONTEXT_MENU_ITEM_DELETE = 0;

    private Bible mBible;
    private SharedPreferences mPreferences;
    private String mCurrentTranslation;

    private TranslationExpandableListAdapter mTranslationListAdapter;

    private View mLoadingSpinner;
    private ExpandableListView mTranslationListView;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        setRetainInstance(true);
        mBible = Bible.getInstance();
        mPreferences = activity.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        mCurrentTranslation = mPreferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_translation_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mLoadingSpinner = view.findViewById(R.id.loading_spinner);

        mTranslationListView = (ExpandableListView) view.findViewById(R.id.translation_list_view);
        mTranslationListAdapter = new TranslationExpandableListAdapter(getActivity(), mCurrentTranslation);
        mTranslationListView.setAdapter(mTranslationListAdapter);
        mTranslationListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                final TranslationInfo translationInfo = mTranslationListAdapter.getChild(groupPosition, childPosition);
                if (translationInfo == null)
                    return false;

                if (groupPosition == TranslationExpandableListAdapter.DOWNLOADED_TRANSLATIONS_GROUP) {
                    Analytics.trackTranslationSelection(translationInfo.shortName);

                    mPreferences.edit()
                            .putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, translationInfo.shortName)
                            .apply();
                    getActivity().finish();
                } else if (groupPosition == TranslationExpandableListAdapter.AVAILABLE_TRANSLATIONS_GROUP) {
                    downloadTranslation(translationInfo.shortName);
                }
                return true;
            }
        });
        registerForContextMenu(mTranslationListView);

        loadTranslations(true);
    }

    private void loadTranslations(final boolean forceRefresh) {
        mLoadingSpinner.setVisibility(View.VISIBLE);
        mTranslationListView.setVisibility(View.GONE);

        mBible.loadTranslations(forceRefresh, new Bible.OnTranslationsLoadedListener() {
            @Override
            public void onTranslationsLoaded(List<TranslationInfo> downloaded, List<TranslationInfo> available) {
                if (downloaded == null || available == null) {
                    DialogHelper.showDialog(getActivity(), false, R.string.dialog_retry,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    loadTranslations(forceRefresh);
                                }
                            }, null
                    );
                    return;
                }

                AnimationHelper.fadeOut(mLoadingSpinner);
                AnimationHelper.fadeIn(mTranslationListView);

                mTranslationListAdapter.setTranslations(downloaded, available);
                mTranslationListAdapter.notifyDataSetChanged();

                final int groupCount = mTranslationListAdapter.getGroupCount();
                for (int i = 0; i < groupCount; ++i)
                    mTranslationListView.expandGroup(i);
            }
        });
    }

    private void downloadTranslation(String translationShortName) {
        ProgressDialogFragment.newInstance(R.string.progress_dialog_translation_downloading, 100)
                .show(getFragmentManager(), TAG_DOWNLOAD_DIALOG_FRAGMENT);

        mBible.downloadTranslation(translationShortName, new Bible.OnTranslationDownloadListener() {
            @Override
            public void onTranslationDownloaded(final String translation, boolean isSuccessful) {
                ((DialogFragment) getFragmentManager().findFragmentByTag(TAG_DOWNLOAD_DIALOG_FRAGMENT)).dismiss();

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
                    DialogHelper.showDialog(getActivity(), true,
                            R.string.dialog_translation_download_failure_message,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    downloadTranslation(translation);
                                }
                            }, null
                    );
                }
            }

            @Override
            public void onTranslationDownloadProgress(String translation, int progress) {
                final DialogFragment fragment = (DialogFragment) getFragmentManager()
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

        final TranslationInfo translationInfo = getTranslationInfo((ExpandableListView.ExpandableListContextMenuInfo) menuInfo);
        if (translationInfo == null || mCurrentTranslation.equals(translationInfo.name))
            return;

        menu.setHeaderTitle(translationInfo.name);
        menu.add(Menu.NONE, CONTEXT_MENU_ITEM_DELETE, Menu.NONE, R.string.action_delete_translation);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListView.ExpandableListContextMenuInfo contextMenuInfo
                = (ExpandableListView.ExpandableListContextMenuInfo) item.getMenuInfo();
        if (contextMenuInfo == null)
            return super.onContextItemSelected(item);

        final TranslationInfo translationInfo = getTranslationInfo(contextMenuInfo);
        if (translationInfo == null)
            return super.onContextItemSelected(item);

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

    private TranslationInfo getTranslationInfo(ExpandableListView.ExpandableListContextMenuInfo contextMenuInfo) {
        if (ExpandableListView.getPackedPositionType(contextMenuInfo.packedPosition)
                != ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            return null;
        }

        final int group = ExpandableListView.getPackedPositionGroup(contextMenuInfo.packedPosition);
        if (group != TranslationExpandableListAdapter.DOWNLOADED_TRANSLATIONS_GROUP)
            return null;

        return mTranslationListAdapter.getChild(
                group, ExpandableListView.getPackedPositionChild(contextMenuInfo.packedPosition));
    }

    private void removeTranslation(String translationShortName) {
        ProgressDialogFragment.newInstance(R.string.progress_dialog_translation_deleting)
                .show(getFragmentManager(), TAG_REMOVE_DIALOG_FRAGMENT);

        mBible.removeTranslation(translationShortName, new Bible.OnTranslationRemovedListener() {
            @Override
            public void onTranslationRemoved(final String translation, boolean isSuccessful) {
                ((DialogFragment) getFragmentManager().findFragmentByTag(TAG_REMOVE_DIALOG_FRAGMENT)).dismiss();

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
}
