package net.zionsoft.obadiah;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
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

        // initializes title bar
        TextView titleBarTextView = (TextView) findViewById(R.id.txtTitle);
        titleBarTextView.setText(R.string.title_download_translation);

        // initializes list view showing available translations
        ListView listView = (ListView) findViewById(R.id.listView);
        m_translationListAdapter = new TranslationDownloadListAdapter(this);
        listView.setAdapter(m_translationListAdapter);
        listView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                m_translationDownloadAsyncTask = new TranslationDownloadAsyncTask();
                m_translationDownloadAsyncTask.execute(position);
            }
        });

        // gets translation list
        new TranslationListDownloadAsyncTask().execute();
    }

    public void onPause()
    {
        super.onPause();

        // cancel existing download async tasks
        if (m_translationDownloadAsyncTask != null)
            m_translationDownloadAsyncTask.cancel(true);
    }

    private class TranslationListDownloadAsyncTask extends AsyncTask<Void, Void, Void>
    {
        protected void onPreExecute()
        {
            // running in the main thread
            m_progressDialog = new ProgressDialog(TranslationDownloadActivity.this);
            m_progressDialog.setCancelable(false);
            m_progressDialog.setMessage(getText(R.string.text_downloading));
            m_progressDialog.show();
        }

        protected Void doInBackground(Void... params)
        {
            // running in the worker thread
            File translationsFile = null;
            try {
                final long lastUpdated = getSharedPreferences("settings", MODE_PRIVATE).getLong("lastUpdated", 0);
                final long now = System.currentTimeMillis();

                if (lastUpdated <= 0 || lastUpdated >= now || ((now - lastUpdated) >= 86400000)) {
                    // no valid local cache, downloads from Internet and writes
                    // to local cache
                    URL url = new URL(BASE_URL + TRANSLATIONS_FILE);
                    HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
                    BufferedInputStream bis = new BufferedInputStream(httpConnection.getInputStream());

                    FileOutputStream fos = new FileOutputStream(new File(getFilesDir(), TRANSLATIONS_FILE));
                    BufferedOutputStream os = new BufferedOutputStream(fos, BUFFER_LENGTH);
                    byte buffer[] = new byte[BUFFER_LENGTH];
                    int read = -1;
                    while ((read = bis.read(buffer, 0, BUFFER_LENGTH)) != -1)
                        os.write(buffer, 0, read);
                    os.flush();
                    os.close();
                }

                // reads from local cache
                translationsFile = new File(getFilesDir() + File.separator + TRANSLATIONS_FILE);
                byte[] buffer = new byte[(int) translationsFile.length()];
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(translationsFile));
                bis.read(buffer);

                // parses the result
                TranslationInfo[] installedTranslations = BibleReader.getInstance().installedTranslations();
                int installedCount = (installedTranslations == null) ? 0 : installedTranslations.length;

                JSONArray replyArray = new JSONArray(new String(buffer, "UTF8"));
                final int length = replyArray.length();

                TranslationInfo[] availableTranslations = new TranslationInfo[length];
                int count = 0;
                for (int i = 0; i < length; ++i) {
                    JSONObject translationObject = replyArray.getJSONObject(i);
                    String path = translationObject.getString("path");
                    int j = 0;
                    for (; j < installedCount; ++j) {
                        if (installedTranslations[j].path.endsWith(path))
                            break;
                    }
                    if (j < installedCount)
                        continue;

                    availableTranslations[count] = new TranslationInfo();
                    availableTranslations[count].name = translationObject.getString("name");
                    availableTranslations[count].path = path;
                    availableTranslations[count].size = translationObject.getInt("size");
                    ++count;
                }

                if (installedCount == 0) {
                    m_availableTranslations = availableTranslations;
                } else if (count > 0) {
                    m_availableTranslations = new TranslationInfo[count];
                    for (int i = 0; i < count; ++i)
                        m_availableTranslations[i] = availableTranslations[i];
                }
            } catch (Exception e) {
                e.printStackTrace();

                m_hasError = true;
                m_availableTranslations = null;
                if (translationsFile != null)
                    translationsFile.delete();
            } finally {
                SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
                editor.putLong("lastUpdated", m_hasError ? 0 : System.currentTimeMillis());
                editor.commit();
            }
            return null;
        }

        protected void onPostExecute(Void result)
        {
            // running in the main thread
            if (m_hasError || m_availableTranslations == null || m_availableTranslations.length == 0) {
                m_progressDialog.dismiss();
                Toast.makeText(
                        TranslationDownloadActivity.this,
                        m_hasError ? R.string.text_fail_to_fetch_translations_list
                                : R.string.text_no_available_translation, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                int length = m_availableTranslations.length;
                String[] texts = new String[length];
                int[] sizes = new int[length];
                for (int i = 0; i < length; ++i) {
                    texts[i] = m_availableTranslations[i].name;
                    sizes[i] = m_availableTranslations[i].size;
                }
                TranslationDownloadActivity.this.m_translationListAdapter.setTexts(texts, sizes);
                m_progressDialog.dismiss();
            }
        }

        private static final int BUFFER_LENGTH = 2048;
        private static final String TRANSLATIONS_FILE = "translations.json";

        private boolean m_hasError;
        private ProgressDialog m_progressDialog;
    }

    private class TranslationDownloadAsyncTask extends AsyncTask<Integer, Integer, Void>
    {
        protected void onPreExecute()
        {
            // running in the main thread
            m_progressDialog = new ProgressDialog(TranslationDownloadActivity.this);
            m_progressDialog.setCancelable(true);
            m_progressDialog.setCanceledOnTouchOutside(false);
            m_progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
            {
                public void onCancel(DialogInterface dialog)
                {
                    TranslationDownloadAsyncTask.this.cancel(true);
                }
            });
            m_progressDialog.setMessage(getText(R.string.text_downloading));
            m_progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            m_progressDialog.setMax(100);
            m_progressDialog.setProgress(0);
            m_progressDialog.show();
        }

        protected Void doInBackground(Integer... positions)
        {
            // running in the worker thread
            try {
                TranslationInfo translationToDownload = m_availableTranslations[positions[0]];
                String path = translationToDownload.path;

                // creates sub-folder
                m_dir = new File(getFilesDir(), path);
                m_dir.mkdir();

                URL url = new URL(BASE_URL + path + ".zip");
                HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(httpConnection.getInputStream()));
                ZipEntry entry;
                byte buffer[] = new byte[BUFFER_LENGTH];
                int read = -1;
                int downloaded = 0;
                while ((entry = zis.getNextEntry()) != null) {
                    if (isCancelled())
                        break;

                    // unzips and writes to internal storage
                    FileOutputStream fos = new FileOutputStream(new File(m_dir, entry.getName()));
                    BufferedOutputStream os = new BufferedOutputStream(fos, BUFFER_LENGTH);
                    while ((read = zis.read(buffer, 0, BUFFER_LENGTH)) != -1)
                        os.write(buffer, 0, read);
                    os.flush();
                    os.close();

                    // notifies the progress
                    publishProgress(++downloaded / 11);
                }
                zis.close();

                // TODO checks if the downloaded translation is valid
            } catch (Exception e) {
                e.printStackTrace();

                Utils.removeDirectory(m_dir);
                m_dir = null;
                m_hasError = true;
            }
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
            Utils.removeDirectory(m_dir);
            m_dir = null;
            TranslationDownloadActivity.this.m_translationDownloadAsyncTask = null;
            m_progressDialog.dismiss();
        }

        protected void onPostExecute(Void result)
        {
            // running in the main thread
            if (m_hasError) {
                m_progressDialog.dismiss();
                Toast.makeText(TranslationDownloadActivity.this, R.string.text_fail_to_fetch_translation,
                        Toast.LENGTH_SHORT).show();
            } else {
                BibleReader.getInstance().refresh();
                m_dir = null;
                TranslationDownloadActivity.this.m_translationDownloadAsyncTask = null;
                m_progressDialog.dismiss();
                finish();
            }
        }

        private static final int BUFFER_LENGTH = 2048;

        private boolean m_hasError;
        private File m_dir;
        private ProgressDialog m_progressDialog;
    }

    private class TranslationDownloadListAdapter extends ListBaseAdapter
    {
        public TranslationDownloadListAdapter(Context context)
        {
            super(context);
        }

        public void setTexts(String[] texts, int[] sizes)
        {
            m_texts = texts;

            final int length = sizes.length;
            m_subTexts = new String[length];
            final String size = TranslationDownloadActivity.this.getResources().getText(R.string.text_size).toString();
            for (int i = 0; i < length; ++i)
                m_subTexts[i] = size + sizes[i] + "KB";

            notifyDataSetChanged();
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            LinearLayout linearLayout;
            if (convertView == null) {
                linearLayout = new LinearLayout(m_context);
                linearLayout.setOrientation(LinearLayout.VERTICAL);

                // first line
                TextView textView = new TextView(m_context);
                textView.setGravity(Gravity.CENTER_VERTICAL);
                textView.setTextColor(Color.BLACK);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
                textView.setPadding(30, 20, 30, 0);
                linearLayout.addView(textView);

                // second line
                textView = new TextView(m_context);
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

        private String[] m_subTexts;
    }

    private static final String BASE_URL = "http://bible.zionsoft.net/";

    private TranslationDownloadListAdapter m_translationListAdapter;
    private TranslationDownloadAsyncTask m_translationDownloadAsyncTask;
    private TranslationInfo[] m_availableTranslations;
}
