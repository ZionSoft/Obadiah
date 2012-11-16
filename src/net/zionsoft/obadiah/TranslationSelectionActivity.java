package net.zionsoft.obadiah;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TranslationSelectionActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.translationselection_activity);

        m_translationManager = new TranslationManager(this);
        m_selectedTranslationShortName = getSharedPreferences(Constants.SETTING_KEY, MODE_PRIVATE).getString(
                Constants.CURRENT_TRANSLATION_SETTING_KEY, null);

        // initializes list view showing installed translations
        m_translationListAdapter = new TranslationSelectionListAdapter(this);
        m_translationListView = (ListView) findViewById(R.id.translation_listview);
        m_translationListView.setAdapter(m_translationListAdapter);
        m_translationListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (position == TranslationSelectionActivity.this.m_translationListAdapter.getCount() - 1) {
                    TranslationSelectionActivity.this.startTranslationDownloadActivity();
                    return;
                }

                final SharedPreferences.Editor editor = TranslationSelectionActivity.this.getSharedPreferences(
                        Constants.SETTING_KEY, MODE_PRIVATE).edit();
                editor.putString(Constants.CURRENT_TRANSLATION_SETTING_KEY, m_installedTranslations[position].shortName);
                editor.commit();

                finish();
            }
        });
        m_translationListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (position == TranslationSelectionActivity.this.m_translationListAdapter.getCount() - 1
                        || TranslationSelectionActivity.this.m_installedTranslations[position].shortName
                                .equals(TranslationSelectionActivity.this.m_selectedTranslationShortName)) {
                    return false;
                }

                final int selectedIndex = position;
                final Resources resources = TranslationSelectionActivity.this.getResources();
                final CharSequence[] items = { resources.getText(R.string.text_delete) };
                final AlertDialog.Builder contextMenuDialogBuilder = new AlertDialog.Builder(
                        TranslationSelectionActivity.this);
                contextMenuDialogBuilder.setItems(items, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        switch (which) {
                        case 0: // delete
                            final AlertDialog.Builder deleteConfirmDialogBuilder = new AlertDialog.Builder(
                                    TranslationSelectionActivity.this);
                            deleteConfirmDialogBuilder.setMessage(resources.getText(R.string.text_delete_confirm))
                                    .setCancelable(true)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int id)
                                        {
                                            new TranslationDeleteAsyncTask()
                                                    .execute(TranslationSelectionActivity.this.m_installedTranslations[selectedIndex].shortName);
                                        }
                                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int id)
                                        {
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

            class TranslationDeleteAsyncTask extends AsyncTask<String, Void, Void>
            {
                protected void onPreExecute()
                {
                    // running in the main thread

                    m_progressDialog = new ProgressDialog(TranslationSelectionActivity.this);
                    m_progressDialog.setCancelable(false);
                    m_progressDialog.setMessage(getText(R.string.text_deleting));
                    m_progressDialog.show();
                }

                protected Void doInBackground(String... params)
                {
                    // running in the worker thread

                    TranslationSelectionActivity.this.m_translationManager.removeTranslation(params[0]);
                    return null;
                }

                protected void onPostExecute(Void result)
                {
                    // running in the main thread

                    TranslationSelectionActivity.this.populateUi();
                    m_progressDialog.cancel();
                    Toast.makeText(TranslationSelectionActivity.this, R.string.text_deleted, Toast.LENGTH_SHORT).show();
                }

                private ProgressDialog m_progressDialog;
            }
        });
    }

    protected void onResume()
    {
        super.onResume();

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(SettingsActivity.PREF_NIGHTMODE, false)) {
            // night mode
            m_translationListView.setBackgroundColor(Color.BLACK);
            m_translationListView.setCacheColorHint(Color.BLACK);
            m_textColor = Color.WHITE;
        } else {
            // day mode
            m_translationListView.setBackgroundColor(Color.WHITE);
            m_translationListView.setCacheColorHint(Color.WHITE);
            m_textColor = Color.BLACK;
        }

        populateUi();
    }

    private void populateUi()
    {
        final TranslationInfo[] translations = m_translationManager.translations();
        int installedTranslationCount = translations == null ? 0 : translations.length;
        if (installedTranslationCount > 0) {
            for (TranslationInfo translationInfo : translations) {
                if (!translationInfo.installed)
                    --installedTranslationCount;
            }
        }

        if (installedTranslationCount == 0) {
            // only directly opens TranslationDownloadActivity once
            if (m_firstTime) {
                startTranslationDownloadActivity();
                m_firstTime = false;
            }
            m_translationListAdapter.setInstalledTranslations(null);
            return;
        }

        final TranslationInfo[] installedTranslations = new TranslationInfo[installedTranslationCount];
        int index = 0;
        for (TranslationInfo translationInfo : translations) {
            if (translationInfo.installed)
                installedTranslations[index++] = translationInfo;
        }

        // 1st time resumed from TranslationDownloadActivity with installed translation
        if (m_selectedTranslationShortName == null)
            m_selectedTranslationShortName = installedTranslations[0].shortName;

        m_translationListAdapter.setInstalledTranslations(installedTranslations);
        m_installedTranslations = installedTranslations;
    }

    private void startTranslationDownloadActivity()
    {
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

    private class TranslationSelectionListAdapter extends ListBaseAdapter
    {
        public TranslationSelectionListAdapter(Context context)
        {
            super(context);
            m_footerText = context.getResources().getString(R.string.text_download);
        }

        public void setInstalledTranslations(TranslationInfo[] translations)
        {
            m_installedTranslations = translations;
            notifyDataSetChanged();
        }

        public int getCount()
        {
            return (m_installedTranslations == null) ? 1 : m_installedTranslations.length + 1;
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            TextView textView;
            if (convertView == null) {
                textView = new TextView(m_context);
                textView.setGravity(Gravity.CENTER_VERTICAL);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        m_context.getResources().getDimension(R.dimen.text_size));
                textView.setPadding(30, 20, 30, 20);
            } else {
                textView = (TextView) convertView;
            }

            textView.setTextColor(TranslationSelectionActivity.this.m_textColor);

            if (m_installedTranslations != null && position < m_installedTranslations.length) {
                textView.setText(m_installedTranslations[position].name);
                if (TranslationSelectionActivity.this.m_selectedTranslationShortName
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

    private boolean m_firstTime = true;
    private int m_textColor;
    private ListView m_translationListView;
    private String m_selectedTranslationShortName;
    private TranslationManager m_translationManager;
    private TranslationSelectionListAdapter m_translationListAdapter;
    private TranslationInfo[] m_installedTranslations;
}
