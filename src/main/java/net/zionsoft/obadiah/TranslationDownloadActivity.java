/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2013 ZionSoft
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

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import net.zionsoft.obadiah.bible.TranslationDownloadService;
import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.bible.TranslationListLoadService;
import net.zionsoft.obadiah.util.SettingsManager;

import java.util.List;

public class TranslationDownloadActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.translationdownload_activity);

        mSettingsManager = new SettingsManager(this);

        initializeUi();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSettingsManager.refresh();
        final int backgroundColor = mSettingsManager.backgroundColor();
        mTranslationListView.setBackgroundColor(backgroundColor);
        mTranslationListView.setCacheColorHint(backgroundColor);
    }

    @Override
    protected void onDestroy() {
        if (mTranslationDownloadProgressDialog != null)
            mTranslationDownloadProgressDialog.dismiss();
        unregisterTranslationListLoadingStatusListener();
        unregisterTranslationDownloadingStatusListener();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_translationdownload, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                loadTranslationList(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // loads translation list

    private boolean isLoadingTranslationList() {
        return getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(Constants.PREF_KEY_LOADING_TRANSLATION_LIST, false);
    }

    private void loadTranslationList(final boolean forceRefresh) {
        registerTranslationListLoadingStatusListener();

        if (!isLoadingTranslationList()) {
            getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putBoolean(Constants.PREF_KEY_LOADING_TRANSLATION_LIST, true).commit();

            mLoadingSpinner.setVisibility(View.VISIBLE);
            mTranslationListView.setVisibility(View.GONE);

            final Intent intent = new Intent(this, TranslationListLoadService.class);
            intent.putExtra(TranslationListLoadService.KEY_FORCE_REFRESH, forceRefresh);
            startService(intent);
        }
    }

    private void registerTranslationListLoadingStatusListener() {
        if (mTranslationListLoadingStatusListener != null)
            return;

        mTranslationListLoadingStatusListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterTranslationListLoadingStatusListener();

                Animator.fadeOut(mLoadingSpinner);
                Animator.fadeIn(mTranslationListView);

                final int status = intent.getIntExtra(TranslationListLoadService.KEY_STATUS,
                        TranslationListLoadService.STATUS_SUCCESS);
                if (status == TranslationListLoadService.STATUS_SUCCESS) {
                    mAvailableTranslations = intent.getParcelableArrayListExtra(
                            TranslationListLoadService.KEY_TRANSLATION_LIST);
                    if (mAvailableTranslations.size() > 0) {
                        mTranslationListAdapter.setTranslations(mAvailableTranslations);
                    } else {
                        Toast.makeText(TranslationDownloadActivity.this,
                                R.string.text_no_available_translation, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    DialogHelper.showDialog(TranslationDownloadActivity.this, false,
                            status == TranslationListLoadService.STATUS_NETWORK_FAILURE
                                    ? R.string.dialog_network_failure_message
                                    : R.string.dialog_translation_list_fetch_failure_message,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    loadTranslationList(true);
                                }
                            },
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            }
                    );
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mTranslationListLoadingStatusListener,
                        new IntentFilter(TranslationListLoadService.ACTION_STATUS_UPDATE));
    }

    private void unregisterTranslationListLoadingStatusListener() {
        if (mTranslationListLoadingStatusListener == null)
            return;

        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mTranslationListLoadingStatusListener);
        mTranslationListLoadingStatusListener = null;
    }


    // downloads translation

    private boolean isDownloadingTranslation() {
        return getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(Constants.PREF_KEY_DOWNLOADING_TRANSLATION, false);
    }

    private void downloadTranslation(TranslationInfo translation) {
        registerTranslationDownloadingStatusListener(translation);

        mTranslationDownloadProgressDialog = new ProgressDialog(this);
        mTranslationDownloadProgressDialog.setCancelable(true);
        mTranslationDownloadProgressDialog.setCanceledOnTouchOutside(false);
        mTranslationDownloadProgressDialog.setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        LocalBroadcastManager.getInstance(TranslationDownloadActivity.this)
                                .sendBroadcast(new Intent(
                                        TranslationDownloadService.ACTION_CANCEL_DOWNLOAD));
                    }
                });
        mTranslationDownloadProgressDialog.setMessage(getText(R.string.text_downloading));
        mTranslationDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mTranslationDownloadProgressDialog.setMax(100);
        mTranslationDownloadProgressDialog.setProgress(0);
        mTranslationDownloadProgressDialog.show();

        if (!isDownloadingTranslation()) {
            getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putBoolean(Constants.PREF_KEY_DOWNLOADING_TRANSLATION, true).commit();

            final Intent intent = new Intent(this, TranslationDownloadService.class);
            intent.putExtra(TranslationDownloadService.KEY_TRANSLATION, translation);
            startService(intent);
        }
    }

    private void registerTranslationDownloadingStatusListener(final TranslationInfo translation) {
        if (mTranslationDownloadingStatusListener != null)
            return;

        mTranslationDownloadingStatusListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final int status = intent.getIntExtra(TranslationDownloadService.KEY_STATUS,
                        TranslationDownloadService.STATUS_SUCCESS);
                if (status == TranslationDownloadService.STATUS_SUCCESS) {
                    unregisterTranslationDownloadingStatusListener();
                    finish();
                } else if (status == TranslationDownloadService.STATUS_IN_PROGRESS) {
                    mTranslationDownloadProgressDialog.setProgress(intent.
                            getIntExtra(TranslationDownloadService.KEY_PROGRESS, 0));
                } else if (status == TranslationDownloadService.STATUS_CANCELED) {
                    unregisterTranslationDownloadingStatusListener();
                    mTranslationDownloadProgressDialog.dismiss();
                } else {
                    unregisterTranslationDownloadingStatusListener();
                    mTranslationDownloadProgressDialog.dismiss();
                    DialogHelper.showDialog(TranslationDownloadActivity.this, false,
                            status == TranslationDownloadService.STATUS_NETWORK_FAILURE
                                    ? R.string.dialog_network_failure_message
                                    : R.string.dialog_translation_download_failure_message,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    downloadTranslation(translation);
                                }
                            },
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            }
                    );
                }
            }
        };
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mTranslationDownloadingStatusListener,
                        new IntentFilter(TranslationDownloadService.ACTION_STATUS_UPDATE));
    }

    private void unregisterTranslationDownloadingStatusListener() {
        if (mTranslationDownloadingStatusListener == null)
            return;

        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mTranslationDownloadingStatusListener);
        mTranslationDownloadingStatusListener = null;
    }


    // UI related

    private void initializeUi() {
        mLoadingSpinner = findViewById(R.id.translation_download_loading_spinner);

        // translation list view
        mTranslationListView = (ListView) findViewById(R.id.translation_listview);
        mTranslationListAdapter = new TranslationDownloadListAdapter(this, mSettingsManager);
        mTranslationListView.setAdapter(mTranslationListAdapter);
        mTranslationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                downloadTranslation(mAvailableTranslations.get(position));
            }
        });

        loadTranslationList(false);
    }

    private BroadcastReceiver mTranslationDownloadingStatusListener;
    private BroadcastReceiver mTranslationListLoadingStatusListener;

    private View mLoadingSpinner;
    private ListView mTranslationListView;
    private ProgressDialog mTranslationDownloadProgressDialog;

    private SettingsManager mSettingsManager;

    private TranslationDownloadListAdapter mTranslationListAdapter;
    private List<TranslationInfo> mAvailableTranslations;
}
