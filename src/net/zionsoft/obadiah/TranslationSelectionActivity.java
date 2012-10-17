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
        m_selectedTranslationShortName = getIntent().getExtras().getString("selectedTranslationShortName");

        // initializes title bar
        final TextView titleBarTextView = (TextView) findViewById(R.id.txtTitle);
        titleBarTextView.setText(R.string.title_select_translation);

        // initializes list view showing installed translations
        m_listAdapter = new TranslationSelectionListAdapter(this);
        final ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(m_listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (position == m_listAdapter.getCount() - 1) {
                    startTranslationDownloadActivity();
                    return;
                }

                final SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
                editor.putString("selectedTranslation", m_installedTranslations[position].shortName);
                editor.commit();

                finish();
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (position == m_listAdapter.getCount() - 1
                        || m_installedTranslations[position].shortName.equals(m_selectedTranslationShortName)) {
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
                                                    .execute(m_installedTranslations[selectedIndex].shortName);
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
                    m_translationManager.removeTranslation(params[0]);
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
            m_listAdapter.setInstalledTranslations(null);
            return;
        }

        final TranslationInfo[] installedTranslations = new TranslationInfo[installedTranslationCount];
        int index = 0;
        for (TranslationInfo translationInfo : translations) {
            if (translationInfo.installed)
                installedTranslations[index++] = translationInfo;
        }
        m_listAdapter.setInstalledTranslations(installedTranslations);
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

    private static class TranslationSelectionListAdapter extends ListBaseAdapter
    {
        public TranslationSelectionListAdapter(TranslationSelectionActivity activity)
        {
            super(activity);
            m_translationSelectionActivity = activity;
            m_footerText = m_translationSelectionActivity.getResources().getString(R.string.text_download);
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
            TranslationSelectionItemTextView textView;
            if (convertView == null)
                textView = new TranslationSelectionItemTextView(m_context);
            else
                textView = (TranslationSelectionItemTextView) convertView;

            if (m_installedTranslations != null && position < m_installedTranslations.length) {
                textView.setText(m_installedTranslations[position].name);
                if (m_translationSelectionActivity.m_selectedTranslationShortName
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

        private static class TranslationSelectionItemTextView extends TextView
        {
            public TranslationSelectionItemTextView(Context context)
            {
                super(context);

                setGravity(Gravity.CENTER_VERTICAL);
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
                setPadding(30, 20, 30, 20);
                setTextColor(Color.BLACK);
            }
        }

        private String m_footerText;
        private TranslationSelectionActivity m_translationSelectionActivity;
        private TranslationInfo[] m_installedTranslations;
    }

    private boolean m_firstTime = true;
    private String m_selectedTranslationShortName;
    private TranslationManager m_translationManager;
    private TranslationSelectionListAdapter m_listAdapter;
    private TranslationInfo[] m_installedTranslations;
}
