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

package net.zionsoft.obadiah;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.bible.TranslationManager;
import net.zionsoft.obadiah.bible.TranslationRemoveService;
import net.zionsoft.obadiah.util.SettingsManager;

import java.util.List;

public class TranslationSelectionActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.translation_selection_activity);

        SettingsManager settingsManager = new SettingsManager(this);
        settingsManager.refresh();

        mTranslationManager = new TranslationManager(this);

        initializeUi(settingsManager);

        mData = (NonConfigurationData) getLastCustomNonConfigurationInstance();
        if (mData == null)
            mData = new NonConfigurationData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        populateUi();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return mData;
    }

    @Override
    protected void onDestroy() {
        unregisterTranslationRemovingStatusListener();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_translation_selection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_download:
                startActivity(new Intent(this, TranslationDownloadActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // loads translation list

    private void loadTranslationList() {
        new AsyncTask<Void, Void, List<TranslationInfo>>() {
            @Override
            protected void onPreExecute() {
                mLoadingSpinner.setVisibility(View.VISIBLE);
                mTranslationListView.setVisibility(View.GONE);
            }

            @Override
            protected List<TranslationInfo> doInBackground(Void... params) {
                return mTranslationManager.installedTranslations();
            }

            @Override
            protected void onPostExecute(List<TranslationInfo> translations) {
                Animator.fadeOut(mLoadingSpinner);
                Animator.fadeIn(mTranslationListView);

                if (translations.size() == 0) {
                    Toast.makeText(TranslationSelectionActivity.this,
                            R.string.toast_no_installed_translation, Toast.LENGTH_SHORT).show();
                    return;
                }

                mData.installedTranslations = translations;
                mTranslationListAdapter.setTranslations(translations);
            }
        }.execute();
    }


    // removes translation

    private boolean isRemovingTranslation() {
        return getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(Constants.PREF_KEY_REMOVING_TRANSLATION, false);
    }

    private void removeTranslation() {
        registerTranslationRemovingStatusListener();

        mTranslationRemoveProgressDialog = new ProgressDialog(TranslationSelectionActivity.this);
        mTranslationRemoveProgressDialog.setCancelable(false);
        mTranslationRemoveProgressDialog.setMessage(getText(R.string.progress_dialog_translation_deleting));
        mTranslationRemoveProgressDialog.show();

        if (!isRemovingTranslation()) {
            getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putBoolean(Constants.PREF_KEY_REMOVING_TRANSLATION, true).commit();

            startService(new Intent(this, TranslationRemoveService.class)
                    .putExtra(TranslationRemoveService.KEY_TRANSLATION, mData.translationToRemove));
        }
    }

    private void registerTranslationRemovingStatusListener() {
        if (mTranslationRemovingStatusListener != null)
            return;

        mTranslationRemovingStatusListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int status = intent.getIntExtra(TranslationRemoveService.KEY_STATUS,
                        TranslationRemoveService.STATUS_SUCCESS);
                if (status == TranslationRemoveService.STATUS_SUCCESS) {
                    unregisterTranslationRemovingStatusListener();

                    mData.installedTranslations.remove(mData.translationToRemove);
                    mTranslationListAdapter.setTranslations(mData.installedTranslations);
                    mData.translationToRemove = null;

                    mTranslationRemoveProgressDialog.dismiss();
                    Toast.makeText(TranslationSelectionActivity.this,
                            R.string.toast_translation_deleted, Toast.LENGTH_SHORT).show();
                } else {
                    DialogHelper.showDialog(TranslationSelectionActivity.this, false,
                            R.string.dialog_translation_remove_failure_message,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    removeTranslation();
                                }
                            },
                            null);
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mTranslationRemovingStatusListener,
                        new IntentFilter(TranslationRemoveService.ACTION_STATUS_UPDATE));
    }

    private void unregisterTranslationRemovingStatusListener() {
        if (mTranslationRemovingStatusListener == null)
            return;

        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mTranslationRemovingStatusListener);
        mTranslationRemovingStatusListener = null;
    }


    // UI related

    private void initializeUi(SettingsManager settingsManager) {
        mLoadingSpinner = findViewById(R.id.translation_selection_loading_spinner);
        getWindow().getDecorView().setBackgroundColor(settingsManager.backgroundColor());

        // translation list view
        mTranslationListAdapter = new TranslationSelectionListAdapter(this, settingsManager);
        mTranslationListView = (ListView) findViewById(R.id.translation_list_view);
        mTranslationListView.setAdapter(mTranslationListAdapter);
        mTranslationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE).edit()
                        .putString(Constants.PREF_KEY_LAST_READ_TRANSLATION,
                                mTranslationListAdapter.getItem(position).shortName())
                        .commit();
                finish();
            }
        });
        mTranslationListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final TranslationInfo selected = mTranslationListAdapter.getItem(position);
                if (selected.shortName().equals(mData.selectedTranslationShortName))
                    return false;

                final CharSequence[] items = {getResources().getText(R.string.action_delete_translation)};
                final AlertDialog.Builder contextMenuDialogBuilder = new AlertDialog.Builder(
                        TranslationSelectionActivity.this);
                contextMenuDialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // delete
                                DialogHelper.showDialog(TranslationSelectionActivity.this, true,
                                        R.string.dialog_translation_delete_confirm_message,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                mData.translationToRemove = selected;
                                                removeTranslation();
                                            }
                                        }, null);
                                break;
                        }
                    }
                });
                contextMenuDialogBuilder.create().show();

                return true;
            }
        });
    }

    private void populateUi() {
        final String selected = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
                .getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
        if (selected.equals(mData.selectedTranslationShortName)) {
            mLoadingSpinner.setVisibility(View.GONE);
            mTranslationListAdapter.setTranslations(mData.installedTranslations);
        } else {
            mData.selectedTranslationShortName = selected;
            loadTranslationList();
        }
        mTranslationListAdapter.setSelectedTranslation(mData.selectedTranslationShortName);
    }

    private static class NonConfigurationData {
        TranslationInfo translationToRemove;
        List<TranslationInfo> installedTranslations;
        String selectedTranslationShortName;
    }

    private BroadcastReceiver mTranslationRemovingStatusListener;

    private NonConfigurationData mData;

    private ListView mTranslationListView;
    private View mLoadingSpinner;
    private ProgressDialog mTranslationRemoveProgressDialog;

    private TranslationManager mTranslationManager;
    private TranslationSelectionListAdapter mTranslationListAdapter;
}
