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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.bible.TranslationManager;
import net.zionsoft.obadiah.util.SettingsManager;

public class TranslationSelectionActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.translationselection_activity);

        mSettingsManager = new SettingsManager(this);
        mTranslationManager = new TranslationManager(this);
        mSelectedTranslationShortName = getSharedPreferences(Constants.SETTING_KEY, MODE_PRIVATE).getString(
                Constants.CURRENT_TRANSLATION_SETTING_KEY, null);

        // initializes list view showing installed translations
        mTranslationListAdapter = new TranslationSelectionListAdapter(this);
        mTranslationListView = (ListView) findViewById(R.id.translation_listview);
        mTranslationListView.setAdapter(mTranslationListAdapter);
        mTranslationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == TranslationSelectionActivity.this.mTranslationListAdapter.getCount() - 1) {
                    TranslationSelectionActivity.this.startTranslationDownloadActivity();
                    return;
                }

                final SharedPreferences.Editor editor = TranslationSelectionActivity.this.getSharedPreferences(
                        Constants.SETTING_KEY, MODE_PRIVATE).edit();
                editor.putString(Constants.CURRENT_TRANSLATION_SETTING_KEY, mInstalledTranslations[position].shortName);
                editor.commit();

                finish();
            }
        });
        mTranslationListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == TranslationSelectionActivity.this.mTranslationListAdapter.getCount() - 1
                        || TranslationSelectionActivity.this.mInstalledTranslations[position].shortName
                        .equals(TranslationSelectionActivity.this.mSelectedTranslationShortName)) {
                    return false;
                }

                final int selectedIndex = position;
                final Resources resources = TranslationSelectionActivity.this.getResources();
                final CharSequence[] items = {resources.getText(R.string.text_delete)};
                final AlertDialog.Builder contextMenuDialogBuilder = new AlertDialog.Builder(
                        TranslationSelectionActivity.this);
                contextMenuDialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // delete
                                final AlertDialog.Builder deleteConfirmDialogBuilder = new AlertDialog.Builder(
                                        TranslationSelectionActivity.this);
                                deleteConfirmDialogBuilder.setMessage(resources.getText(R.string.text_delete_confirm))
                                        .setCancelable(true)
                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                new TranslationDeleteAsyncTask()
                                                        .execute(TranslationSelectionActivity.this.mInstalledTranslations[selectedIndex].shortName);
                                            }
                                        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                    }
                                });
                                deleteConfirmDialogBuilder.create().show();
                                break;
                        }
                    }
                });
                contextMenuDialogBuilder.create().show();

                return true;
            }

            class TranslationDeleteAsyncTask extends AsyncTask<String, Void, Void> {
                protected void onPreExecute() {
                    // running in the main thread

                    m_progressDialog = new ProgressDialog(TranslationSelectionActivity.this);
                    m_progressDialog.setCancelable(false);
                    m_progressDialog.setMessage(getText(R.string.text_deleting));
                    m_progressDialog.show();
                }

                protected Void doInBackground(String... params) {
                    // running in the worker thread

                    TranslationSelectionActivity.this.mTranslationManager.removeTranslation(params[0]);
                    return null;
                }

                protected void onPostExecute(Void result) {
                    // running in the main thread

                    TranslationSelectionActivity.this.populateUi();
                    m_progressDialog.cancel();
                    Toast.makeText(TranslationSelectionActivity.this, R.string.text_deleted, Toast.LENGTH_SHORT).show();
                }

                private ProgressDialog m_progressDialog;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSettingsManager.refresh();
        final int backgroundColor = mSettingsManager.backgroundColor();
        mTranslationListView.setBackgroundColor(backgroundColor);
        mTranslationListView.setCacheColorHint(backgroundColor);
        mTextColor = mSettingsManager.textColor();

        populateUi();
    }

    private void populateUi() {
        final TranslationInfo[] translations = mTranslationManager.translations();
        int installedTranslationCount = translations == null ? 0 : translations.length;
        if (installedTranslationCount > 0) {
            for (TranslationInfo translationInfo : translations) {
                if (!translationInfo.installed)
                    --installedTranslationCount;
            }
        }

        if (installedTranslationCount == 0) {
            // only directly opens TranslationDownloadActivity once
            if (mFirstTime) {
                startTranslationDownloadActivity();
                mFirstTime = false;
            }
            mTranslationListAdapter.setInstalledTranslations(null);
            return;
        }

        final TranslationInfo[] installedTranslations = new TranslationInfo[installedTranslationCount];
        int index = 0;
        for (TranslationInfo translationInfo : translations) {
            if (translationInfo.installed)
                installedTranslations[index++] = translationInfo;
        }

        // 1st time resumed from TranslationDownloadActivity with installed translation
        if (mSelectedTranslationShortName == null)
            mSelectedTranslationShortName = installedTranslations[0].shortName;

        mTranslationListAdapter.setInstalledTranslations(installedTranslations);
        mInstalledTranslations = installedTranslations;
    }

    private void startTranslationDownloadActivity() {
        // checks connectivity
        final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(TranslationSelectionActivity.this, R.string.text_no_network, Toast.LENGTH_LONG).show();
            return;
        }

        // HTTP connection reuse was buggy before Froyo (i.e. Android 2.2, API Level 8)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO)
            System.setProperty("http.keepAlive", "false");

        startActivity(new Intent(TranslationSelectionActivity.this, TranslationDownloadActivity.class));
    }

    private class TranslationSelectionListAdapter extends ListBaseAdapter {
        public TranslationSelectionListAdapter(Context context) {
            super(context);
            m_footerText = context.getResources().getString(R.string.text_download);
        }

        public void setInstalledTranslations(TranslationInfo[] translations) {
            m_installedTranslations = translations;
            notifyDataSetChanged();
        }

        public int getCount() {
            return (m_installedTranslations == null) ? 1 : m_installedTranslations.length + 1;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                textView = (TextView) View.inflate(mContext,
                        R.layout.translation_selection_list_item, null);
            } else {
                textView = (TextView) convertView;
            }

            textView.setTextColor(TranslationSelectionActivity.this.mTextColor);

            if (m_installedTranslations != null && position < m_installedTranslations.length) {
                textView.setText(m_installedTranslations[position].name);
                if (TranslationSelectionActivity.this.mSelectedTranslationShortName
                        .equals(m_installedTranslations[position].shortName)) {
                    textView.setTypeface(null, Typeface.BOLD);
                    textView.setBackgroundResource(R.drawable.list_item_background_selected);
                } else {
                    textView.setTypeface(null, Typeface.NORMAL);
                    textView.setBackgroundResource(R.drawable.list_item_background);
                }
            } else {
                textView.setText(m_footerText);
                textView.setTypeface(null, Typeface.NORMAL);
                textView.setBackgroundResource(R.drawable.list_item_background);
            }

            return textView;
        }

        private String m_footerText;
        private TranslationInfo[] m_installedTranslations;
    }

    private boolean mFirstTime = true;
    private int mTextColor;
    private ListView mTranslationListView;
    private SettingsManager mSettingsManager;
    private String mSelectedTranslationShortName;
    private TranslationManager mTranslationManager;
    private TranslationSelectionListAdapter mTranslationListAdapter;
    private TranslationInfo[] mInstalledTranslations;
}
