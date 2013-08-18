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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.bible.TranslationManager;
import net.zionsoft.obadiah.util.SettingsManager;

import java.util.List;

public class TranslationSelectionActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.translationselection_activity);

        mSettingsManager = new SettingsManager(this);
        mTranslationManager = new TranslationManager(this);
        mSelectedTranslationShortName = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
                .getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);

        initializeUi();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSettingsManager.refresh();
        final int backgroundColor = mSettingsManager.backgroundColor();
        mTranslationListView.setBackgroundColor(backgroundColor);
        mTranslationListView.setCacheColorHint(backgroundColor);

        populateUi();
    }

    @Override
    protected void onPause() {
        getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE).edit()
                .putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, mSelectedTranslationShortName)
                .commit();
        super.onPause();
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

    private void initializeUi() {
        mLoadingSpinner = findViewById(R.id.translation_selection_loading_spinner);

        // translation list view
        mTranslationListAdapter = new TranslationSelectionListAdapter(this, mSettingsManager);
        mTranslationListAdapter.setSelectedTranslation(mSelectedTranslationShortName);
        mTranslationListView = (ListView) findViewById(R.id.translation_list_view);
        mTranslationListView.setAdapter(mTranslationListAdapter);
        mTranslationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectedTranslationShortName = mTranslationListAdapter.getItem(position).shortName;
                finish();
            }
        });
        mTranslationListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final TranslationInfo selected = mTranslationListAdapter.getItem(position);
                if (selected.shortName.equals(mSelectedTranslationShortName))
                    return false;

                final CharSequence[] items = {getResources().getText(R.string.text_delete)};
                final AlertDialog.Builder contextMenuDialogBuilder = new AlertDialog.Builder(
                        TranslationSelectionActivity.this);
                contextMenuDialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // delete
                                DialogHelper.showDialog(TranslationSelectionActivity.this, true,
                                        R.string.text_delete_confirm,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                removeTranslation(selected);
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
                if (translations.size() == 0) {
                    if (mFirstTime) {
                        startActivity(new Intent(TranslationSelectionActivity.this,
                                TranslationDownloadActivity.class));
                        mFirstTime = false;
                    }
                    return;
                }

                Animator.fadeOut(mLoadingSpinner);
                Animator.fadeIn(mTranslationListView);

                // 1st time resumed from TranslationDownloadActivity with installed translation
                if (mSelectedTranslationShortName == null)
                    mSelectedTranslationShortName = translations.get(0).shortName;

                mTranslationListAdapter.setTranslations(translations);
            }
        }.execute();
    }

    private void removeTranslation(TranslationInfo translation) {
        new AsyncTask<TranslationInfo, Void, Void>() {
            protected void onPreExecute() {
                // running in the main thread

                mProgressDialog = new ProgressDialog(TranslationSelectionActivity.this);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setMessage(getText(R.string.text_deleting));
                mProgressDialog.show();
            }

            protected Void doInBackground(TranslationInfo... params) {
                // running in the worker thread

                mTranslationManager.removeTranslation(params[0]);
                return null;
            }

            protected void onPostExecute(Void result) {
                // running in the main thread

                populateUi();
                mProgressDialog.cancel();
                Toast.makeText(TranslationSelectionActivity.this,
                        R.string.text_deleted, Toast.LENGTH_SHORT).show();
            }

            private ProgressDialog mProgressDialog;
        }.execute(translation);
    }

    private boolean mFirstTime = true;

    private ListView mTranslationListView;
    private View mLoadingSpinner;

    private SettingsManager mSettingsManager;
    private String mSelectedTranslationShortName;
    private TranslationManager mTranslationManager;
    private TranslationSelectionListAdapter mTranslationListAdapter;
}
