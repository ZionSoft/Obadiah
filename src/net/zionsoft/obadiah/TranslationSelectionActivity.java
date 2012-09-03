package net.zionsoft.obadiah;

import java.io.File;

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

        // initializes title bar
        TextView titleBarTextView = (TextView) findViewById(R.id.txtTitle);
        titleBarTextView.setText(R.string.title_select_translation);

        // initializes list view showing installed translations
        m_listAdapter = new TranslationSelectionListAdapter(this);
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(m_listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (position == m_listAdapter.getCount() - 1) {
                    startTranslationDownloadActivity();
                    return;
                }

                BibleReader bibleReader = BibleReader.getInstance();
                String selectedTranslation = bibleReader.installedTranslations()[position].path;
                SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
                editor.putString("selectedTranslation", selectedTranslation);
                editor.commit();

                bibleReader.selectTranslation(selectedTranslation);

                finish();
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (position == m_selectedTranslationIndex || position == m_listAdapter.getCount() - 1)
                    return false;

                final int selectedIndex = position;
                final Resources resources = TranslationSelectionActivity.this.getResources();
                final CharSequence[] items = { resources.getText(R.string.text_delete) };
                AlertDialog.Builder contextMenuDialogBuilder = new AlertDialog.Builder(
                        TranslationSelectionActivity.this);
                contextMenuDialogBuilder.setItems(items, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        switch (which) {
                        case 0: // delete
                            AlertDialog.Builder deleteConfirmDialogBuilder = new AlertDialog.Builder(
                                    TranslationSelectionActivity.this);
                            deleteConfirmDialogBuilder.setMessage(resources.getText(R.string.text_delete_confirm))
                                    .setCancelable(true)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int id)
                                        {
                                            new TranslationDeleteAsyncTask().execute(new File(BibleReader.getInstance()
                                                    .installedTranslations()[selectedIndex].path));
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

            class TranslationDeleteAsyncTask extends AsyncTask<File, Void, Void>
            {
                protected void onPreExecute()
                {
                    // running in the main thread
                    m_progressDialog = new ProgressDialog(TranslationSelectionActivity.this);
                    m_progressDialog.setCancelable(false);
                    m_progressDialog.setMessage(getText(R.string.text_deleting));
                    m_progressDialog.show();
                }

                protected Void doInBackground(File... params)
                {
                    // running in the worker thread
                    Utils.removeDirectory(params[0]);
                    return null;
                }

                protected void onPostExecute(Void result)
                {
                    // running in the main thread
                    BibleReader.getInstance().refresh();
                    TranslationSelectionActivity.this.refresh();
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

        refresh();
    }

    private void refresh()
    {
        BibleReader bibleReader = BibleReader.getInstance();
        TranslationInfo[] installedTranslations = bibleReader.installedTranslations();
        m_selectedTranslationIndex = -1;
        final int translationCount = (installedTranslations == null) ? 0 : installedTranslations.length;
        if (translationCount == 0) {
            // only directly opens TranslationDownloadActivity once
            if (m_firstTime) {
                startTranslationDownloadActivity();
                m_firstTime = false;
            }
            m_listAdapter.setTexts(null); // adds the footer
            return;
        }

        TranslationInfo selectedTranslation = bibleReader.selectedTranslation();
        String[] translationNames = new String[translationCount];
        for (int i = 0; i < translationCount; ++i) {
            translationNames[i] = installedTranslations[i].name;
            if (m_selectedTranslationIndex == -1 && selectedTranslation != null
                    && selectedTranslation.name.equals(installedTranslations[i].name)) {
                m_selectedTranslationIndex = i;
            }
        }
        m_listAdapter.setTexts(translationNames);
    }

    private void startTranslationDownloadActivity()
    {
        // checks connectivity
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(TranslationSelectionActivity.this, R.string.text_no_network, Toast.LENGTH_LONG).show();
            return;
        }

        // HTTP connection reuse was buggy before Froyo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO)
            System.setProperty("http.keepAlive", "false");

        Intent intent = new Intent(TranslationSelectionActivity.this, TranslationDownloadActivity.class);
        startActivity(intent);
    }

    private class TranslationSelectionListAdapter extends ListBaseAdapter
    {
        public TranslationSelectionListAdapter(Context context)
        {
            super(context);
        }

        public void setTexts(String[] texts)
        {
            int length = (texts == null) ? 0 : texts.length;
            m_texts = new String[length + 1];
            for (int i = 0; i < length; ++i)
                m_texts[i] = texts[i];

            m_texts[length] = m_context.getResources().getString(R.string.text_download);

            notifyDataSetChanged();
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            TranslationSelectionItemTextView textView;
            if (convertView == null)
                textView = new TranslationSelectionItemTextView(m_context);
            else
                textView = (TranslationSelectionItemTextView) convertView;

            String text = m_texts[position];
            textView.setText(text);
            if (m_selectedTranslationIndex == position) {
                textView.setTypeface(null, Typeface.BOLD);
                textView.setBackgroundResource(R.drawable.list_item_background_selected);
            } else {
                textView.setTypeface(null, Typeface.NORMAL);
                textView.setBackgroundResource(R.drawable.list_item_background);
            }
            return textView;
        }

        private class TranslationSelectionItemTextView extends TextView
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
    }

    private boolean m_firstTime = true;
    private int m_selectedTranslationIndex;
    private TranslationSelectionListAdapter m_listAdapter;
}
