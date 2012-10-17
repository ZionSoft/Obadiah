package net.zionsoft.obadiah;

import java.io.BufferedInputStream;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class TranslationDownloadActivity extends Activity
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.translationdownload_activity);

        m_translationManager = new TranslationManager(this);

        // initializes title bar
        final TextView titleBarTextView = (TextView) findViewById(R.id.txtTitle);
        titleBarTextView.setText(R.string.title_download_translation);

        // initializes list view showing available translations
        final ListView listView = (ListView) findViewById(R.id.listView);
        m_translationListAdapter = new TranslationDownloadListAdapter(this);
        listView.setAdapter(m_translationListAdapter);
        listView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                m_translationDownloadAsyncTask = new TranslationDownloadAsyncTask(TranslationDownloadActivity.this);
                m_translationDownloadAsyncTask.execute(position);
            }
        });

        // gets translation list
        new TranslationListDownloadAsyncTask(this).execute();
    }

    public void onPause()
    {
        super.onPause();

        // cancel existing download async tasks
        if (m_translationDownloadAsyncTask != null)
            m_translationDownloadAsyncTask.cancel(true);
    }

    private static class TranslationListDownloadAsyncTask extends AsyncTask<Void, Void, Void>
    {
        public TranslationListDownloadAsyncTask(TranslationDownloadActivity activity)
        {
            super();
            m_translationDownloadActivity = activity;
        }

        protected void onPreExecute()
        {
            // running in the main thread
            m_progressDialog = new ProgressDialog(m_translationDownloadActivity);
            m_progressDialog.setCancelable(false);
            m_progressDialog.setMessage(m_translationDownloadActivity.getText(R.string.text_downloading));
            m_progressDialog.show();
        }

        protected Void doInBackground(Void... params)
        {
            // running in the worker thread
            try {
                final long lastUpdated = m_translationDownloadActivity.getSharedPreferences("settings", MODE_PRIVATE)
                        .getLong("lastUpdated", 0);
                final long now = System.currentTimeMillis();
                if (lastUpdated <= 0 || lastUpdated >= now || ((now - lastUpdated) >= 86400000)) {
                    // no valid local cache, i.e. not fetched before, or last fetched more than 1 day ago

                    // downloads from Internet
                    final URL url = new URL(BASE_URL + "list.json");
                    final BufferedInputStream bis = new BufferedInputStream(url.openConnection().getInputStream());
                    final byte[] buffer = new byte[bis.available()];
                    bis.read(buffer);
                    bis.close();

                    // parses the result, and updates the database
                    final JSONArray replyArray = new JSONArray(new String(buffer, "UTF8"));
                    final int length = replyArray.length();
                    final TranslationInfo[] allTranslations = new TranslationInfo[length];
                    for (int i = 0; i < length; ++i) {
                        final JSONObject translationObject = replyArray.getJSONObject(i);
                        final TranslationInfo translationInfo = new TranslationInfo();
                        translationInfo.installed = false;
                        translationInfo.name = translationObject.getString("name");
                        translationInfo.shortName = translationObject.getString("shortName");
                        translationInfo.language = translationObject.getString("language");
                        translationInfo.size = translationObject.getInt("size");
                        allTranslations[i] = translationInfo;
                    }
                    m_translationDownloadActivity.m_translationManager.addTranslations(allTranslations);
                }

                // sets the available translations
                final TranslationInfo[] allTranslations = m_translationDownloadActivity.m_translationManager
                        .translations();
                int availableTranslationsCount = 0;
                for (int i = 0; i < allTranslations.length; ++i) {
                    if (!allTranslations[i].installed)
                        ++availableTranslationsCount;
                }
                if (availableTranslationsCount == 0) {
                    m_translationDownloadActivity.m_availableTranslations = null;
                } else if (availableTranslationsCount == allTranslations.length) {
                    m_translationDownloadActivity.m_availableTranslations = allTranslations;
                } else {
                    final TranslationInfo[] availableTranslations = new TranslationInfo[availableTranslationsCount];
                    int index = 0;
                    for (int i = 0; i < allTranslations.length; ++i) {
                        if (!allTranslations[i].installed)
                            availableTranslations[index++] = allTranslations[i];
                    }
                    m_translationDownloadActivity.m_availableTranslations = availableTranslations;
                }
            } catch (Exception e) {
                e.printStackTrace();

                m_hasError = true;
                m_translationDownloadActivity.m_availableTranslations = null;
            } finally {
                final SharedPreferences.Editor editor = m_translationDownloadActivity.getSharedPreferences("settings",
                        MODE_PRIVATE).edit();
                editor.putLong(
                        "lastUpdated",
                        m_hasError || m_translationDownloadActivity.m_availableTranslations == null ? 0 : System
                                .currentTimeMillis());
                editor.commit();
            }
            return null;
        }

        protected void onPostExecute(Void result)
        {
            // running in the main thread
            if (m_hasError || m_translationDownloadActivity.m_availableTranslations == null
                    || m_translationDownloadActivity.m_availableTranslations.length == 0) {
                m_progressDialog.dismiss();
                Toast.makeText(
                        m_translationDownloadActivity,
                        m_hasError ? R.string.text_fail_to_fetch_translations_list
                                : R.string.text_no_available_translation, Toast.LENGTH_SHORT).show();
                m_translationDownloadActivity.finish();
            } else {
                final int length = m_translationDownloadActivity.m_availableTranslations.length;
                final String[] texts = new String[length];
                final int[] sizes = new int[length];
                for (int i = 0; i < length; ++i) {
                    texts[i] = m_translationDownloadActivity.m_availableTranslations[i].name;
                    sizes[i] = m_translationDownloadActivity.m_availableTranslations[i].size;
                }
                m_translationDownloadActivity.m_translationListAdapter.setTexts(texts, sizes);
                m_progressDialog.dismiss();
            }
        }

        private boolean m_hasError;
        private ProgressDialog m_progressDialog;
        private TranslationDownloadActivity m_translationDownloadActivity;
    }

    protected static class TranslationDownloadAsyncTask extends AsyncTask<Integer, Integer, Void>
    {
        public TranslationDownloadAsyncTask(TranslationDownloadActivity activity)
        {
            super();
            m_translationDownloadActivity = activity;
        }

        public void updateProgress(int progress)
        {
            publishProgress(progress);
        }

        protected void onPreExecute()
        {
            // running in the main thread
            m_progressDialog = new ProgressDialog(m_translationDownloadActivity);
            m_progressDialog.setCancelable(true);
            m_progressDialog.setCanceledOnTouchOutside(false);
            m_progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
            {
                public void onCancel(DialogInterface dialog)
                {
                    TranslationDownloadAsyncTask.this.cancel(true);
                }
            });
            m_progressDialog.setMessage(m_translationDownloadActivity.getText(R.string.text_downloading));
            m_progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            m_progressDialog.setMax(100);
            m_progressDialog.setProgress(0);
            m_progressDialog.show();
        }

        protected Void doInBackground(Integer... positions)
        {
            // running in the worker thread
            final TranslationInfo translationToDownload = m_translationDownloadActivity.m_availableTranslations[positions[0]];
            m_hasError = !m_translationDownloadActivity.m_translationManager.installTranslation(this,
                    translationToDownload);
            return null;
        }

        protected void onProgressUpdate(Integer... progress)
        {
            // running in the main thread
            m_progressDialog.setProgress(progress[0]);
        }

        protected void onCancelled()
        {
            // running in the main thread
            m_progressDialog.dismiss();
        }

        protected void onPostExecute(Void result)
        {
            // running in the main thread
            if (m_hasError) {
                m_progressDialog.dismiss();
                Toast.makeText(m_translationDownloadActivity, R.string.text_fail_to_fetch_translation,
                        Toast.LENGTH_SHORT).show();
            } else {
                m_progressDialog.dismiss();
                m_translationDownloadActivity.finish();
            }
        }

        private boolean m_hasError;
        private ProgressDialog m_progressDialog;
        private TranslationDownloadActivity m_translationDownloadActivity;
    }

    private static class TranslationDownloadListAdapter extends ListBaseAdapter
    {
        public TranslationDownloadListAdapter(TranslationDownloadActivity activity)
        {
            super(activity);
            m_translationDownloadActivity = activity;
        }

        public void setTexts(String[] texts, int[] sizes)
        {
            m_texts = texts;

            final String size = m_translationDownloadActivity.getResources().getText(R.string.text_size).toString();
            final int length = sizes.length;
            m_subTexts = new String[length];
            for (int i = 0; i < length; ++i)
                m_subTexts[i] = size + sizes[i] + "KB";

            notifyDataSetChanged();
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            LinearLayout linearLayout;
            if (convertView == null) {
                linearLayout = new LinearLayout(m_translationDownloadActivity);
                linearLayout.setOrientation(LinearLayout.VERTICAL);

                // first line
                TextView textView = new TextView(m_translationDownloadActivity);
                textView.setGravity(Gravity.CENTER_VERTICAL);
                textView.setTextColor(Color.BLACK);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
                textView.setPadding(30, 20, 30, 0);
                linearLayout.addView(textView);

                // second line
                textView = new TextView(m_translationDownloadActivity);
                textView.setGravity(Gravity.CENTER_VERTICAL);
                textView.setTextColor(Color.GRAY);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                textView.setPadding(30, 0, 30, 20);
                linearLayout.addView(textView);
            } else {
                linearLayout = (LinearLayout) convertView;
            }

            // first line
            TextView textView = (TextView) linearLayout.getChildAt(0);
            textView.setText(m_texts[position]);

            // second line
            textView = (TextView) linearLayout.getChildAt(1);
            textView.setText(m_subTexts[position]);

            return linearLayout;
        }

        private TranslationDownloadActivity m_translationDownloadActivity;
        private String[] m_subTexts;
    }

    protected static final String BASE_URL = "http://bible.zionsoft.net/translations/";

    private TranslationDownloadListAdapter m_translationListAdapter;
    private TranslationDownloadAsyncTask m_translationDownloadAsyncTask;
    private TranslationManager m_translationManager;
    private TranslationInfo[] m_availableTranslations;
}
