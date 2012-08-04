package net.zionsoft.obadiah;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class TranslationDownloadActivity extends Activity
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_translationdownload_activity);
        setTitle(R.string.title_download_translation);

        ListView listView = (ListView) findViewById(R.id.listView);
        m_adapter = new SelectionListAdapter(this);
        listView.setAdapter(m_adapter);
        listView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                new TranslationDownloadAsyncTask().execute(position);
            }
        });

        new TranslationListDownloadAsyncTask().execute();
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
            try {
                // the translation list is cached for 24 hours
                byte[] buffer = null;
                final long lastUpdated = getSharedPreferences("settings", MODE_PRIVATE).getLong("lastUpdated", 0);
                final long now = System.currentTimeMillis();

                if (lastUpdated <= 0 || lastUpdated >= now || ((now - lastUpdated) >= 86400000)) {
                    URL url = new URL(BASE_URL + TRANSLATIONS_FILE);
                    HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(httpConnection.getInputStream());
                    buffer = new byte[in.available()];
                    in.read(buffer);
                    in.close();

                    // saves to internal storage
                    FileOutputStream fos = new FileOutputStream(new File(getFilesDir(), TRANSLATIONS_FILE));
                    BufferedOutputStream os = new BufferedOutputStream(fos);
                    os.write(buffer, 0, buffer.length);
                    os.flush();
                    os.close();
                } else {
                    FileInputStream fis = new FileInputStream(getFilesDir() + File.separator + TRANSLATIONS_FILE);
                    BufferedInputStream is = new BufferedInputStream(fis);
                    int length = is.available();
                    buffer = new byte[length];
                    is.read(buffer);
                }

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
            }
            return null;
        }

        protected void onPostExecute(Void result)
        {
            // running in the main thread
            SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
            editor.putLong("lastUpdated", System.currentTimeMillis());
            editor.commit();

            if (m_availableTranslations == null || m_availableTranslations.length == 0) {
                m_progressDialog.dismiss();
                Toast.makeText(TranslationDownloadActivity.this, R.string.text_no_available_translation,
                        Toast.LENGTH_SHORT).show();
                finish();
            } else {
                int length = m_availableTranslations.length;
                String[] texts = new String[length];
                for (int i = 0; i < length; ++i)
                    texts[i] = m_availableTranslations[i].name + " (" + m_availableTranslations[i].size + " KB)";
                m_adapter.setTexts(texts);
                m_progressDialog.dismiss();
            }
        }

        private static final String TRANSLATIONS_FILE = "translations.json";

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
            } catch (Exception e) {
                e.printStackTrace();
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
            if (m_dir != null) {
                File[] files = m_dir.listFiles();
                for (File file : files)
                    file.delete();
                m_dir.delete();
                m_dir = null;
            }
        }

        protected void onPostExecute(Void result)
        {
            // running in the main thread
            m_dir = null;
            BibleReader.getInstance().refresh();
            m_progressDialog.dismiss();
            finish();
        }

        private static final int BUFFER_LENGTH = 2048;

        private File m_dir;
        private ProgressDialog m_progressDialog;
    }

    private static final String BASE_URL = "http://bible.zionsoft.net/";

    private SelectionListAdapter m_adapter;
    private TranslationInfo[] m_availableTranslations;
}
