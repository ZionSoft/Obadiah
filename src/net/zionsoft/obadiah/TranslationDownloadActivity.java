package net.zionsoft.obadiah;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.translationdownload_activity);

        m_translationManager = new TranslationManager(this);

        // initializes list view showing available translations
        m_translationListView = (ListView) findViewById(R.id.translation_listview);
        m_translationListAdapter = new TranslationListAdapter(this);
        m_translationListView.setAdapter(m_translationListAdapter);
        m_translationListView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                TranslationDownloadActivity.this.m_translationDownloadAsyncTask = new TranslationDownloadAsyncTask();
                TranslationDownloadActivity.this.m_translationDownloadAsyncTask.execute(position);
            }
        });

        // gets translation list
        new TranslationListDownloadAsyncTask().execute(false);
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
    }

    protected void onPause()
    {
        super.onPause();

        // cancel existing download async tasks
        if (m_translationDownloadAsyncTask != null)
            m_translationDownloadAsyncTask.cancel(true);
    }

    public void refresh(View view)
    {
        new TranslationListDownloadAsyncTask().execute(true);
    }

    private class TranslationListDownloadAsyncTask extends AsyncTask<Boolean, Void, Void>
    {
        protected void onPreExecute()
        {
            // running in the main thread

            m_progressDialog = new ProgressDialog(TranslationDownloadActivity.this);
            m_progressDialog.setCancelable(false);
            m_progressDialog.setMessage(TranslationDownloadActivity.this.getText(R.string.text_downloading));
            m_progressDialog.show();
        }

        protected Void doInBackground(Boolean... params)
        {
            // running in the worker thread

            try {
                final long lastUpdated = TranslationDownloadActivity.this
                        .getSharedPreferences("settings", MODE_PRIVATE).getLong("lastUpdated", 0);
                final long now = System.currentTimeMillis();
                if (params[0] || lastUpdated <= 0 || lastUpdated >= now || ((now - lastUpdated) >= 86400000)) {
                    // force update
                    // or no valid local cache, i.e. not fetched before, or last fetched more than 1 day ago

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
                    TranslationDownloadActivity.this.m_translationManager.addTranslations(allTranslations);
                }

                // sets the available translations
                final TranslationInfo[] allTranslations = TranslationDownloadActivity.this.m_translationManager
                        .translations();
                int availableTranslationsCount = 0;
                for (int i = 0; i < allTranslations.length; ++i) {
                    if (!allTranslations[i].installed)
                        ++availableTranslationsCount;
                }
                if (availableTranslationsCount == 0) {
                    TranslationDownloadActivity.this.m_availableTranslations = null;
                } else if (availableTranslationsCount == allTranslations.length) {
                    TranslationDownloadActivity.this.m_availableTranslations = allTranslations;
                } else {
                    final TranslationInfo[] availableTranslations = new TranslationInfo[availableTranslationsCount];
                    int index = 0;
                    for (int i = 0; i < allTranslations.length; ++i) {
                        if (!allTranslations[i].installed)
                            availableTranslations[index++] = allTranslations[i];
                    }
                    TranslationDownloadActivity.this.m_availableTranslations = availableTranslations;
                }
            } catch (Exception e) {
                e.printStackTrace();

                m_hasError = true;
                TranslationDownloadActivity.this.m_availableTranslations = null;
            } finally {
                final SharedPreferences.Editor editor = TranslationDownloadActivity.this.getSharedPreferences(
                        "settings", MODE_PRIVATE).edit();
                if (m_hasError || TranslationDownloadActivity.this.m_availableTranslations == null)
                    editor.putLong("lastUpdated", 0);
                else
                    editor.putLong("lastUpdated", System.currentTimeMillis());
                editor.commit();
            }
            return null;
        }

        protected void onPostExecute(Void result)
        {
            // running in the main thread

            m_progressDialog.dismiss();

            if (m_hasError || TranslationDownloadActivity.this.m_availableTranslations == null
                    || TranslationDownloadActivity.this.m_availableTranslations.length == 0) {
                // either error occurs, or no available translations
                Toast.makeText(
                        TranslationDownloadActivity.this,
                        m_hasError ? R.string.text_fail_to_fetch_translations_list
                                : R.string.text_no_available_translation, Toast.LENGTH_SHORT).show();
                TranslationDownloadActivity.this.finish();
            } else {
                // everything fine with available translations
                final int length = TranslationDownloadActivity.this.m_availableTranslations.length;
                final String[] texts = new String[length];
                final int[] sizes = new int[length];
                for (int i = 0; i < length; ++i) {
                    texts[i] = TranslationDownloadActivity.this.m_availableTranslations[i].name;
                    sizes[i] = TranslationDownloadActivity.this.m_availableTranslations[i].size;
                }
                TranslationDownloadActivity.this.m_translationListAdapter.setTexts(texts, sizes);
            }
        }

        private boolean m_hasError;
        private ProgressDialog m_progressDialog;
    }

    protected class TranslationDownloadAsyncTask extends AsyncTask<Integer, Integer, Void>
    {
        public void updateProgress(int progress)
        {
            publishProgress(progress);
        }

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
            m_progressDialog.setMessage(TranslationDownloadActivity.this.getText(R.string.text_downloading));
            m_progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            m_progressDialog.setMax(100);
            m_progressDialog.setProgress(0);
            m_progressDialog.show();
        }

        protected Void doInBackground(Integer... positions)
        {
            // running in the worker thread

            // the logic should be in TranslationManager

            final TranslationInfo translationToDownload = TranslationDownloadActivity.this.m_availableTranslations[positions[0]];
            final SQLiteDatabase db = new TranslationsDatabaseHelper(TranslationDownloadActivity.this)
                    .getWritableDatabase();
            db.beginTransaction();
            try {
                // creates a translation table
                db.execSQL("CREATE TABLE " + translationToDownload.shortName + " ("
                        + TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + " INTEGER NOT NULL, "
                        + TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX + " INTEGER NOT NULL, "
                        + TranslationsDatabaseHelper.COLUMN_VERSE_INDEX + " INTEGER NOT NULL, "
                        + TranslationsDatabaseHelper.COLUMN_TEXT + " TEXT NOT NULL);");
                db.execSQL("CREATE INDEX INDEX_" + translationToDownload.shortName + " ON "
                        + translationToDownload.shortName + " (" + TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + ", "
                        + TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX + ", "
                        + TranslationsDatabaseHelper.COLUMN_VERSE_INDEX + ");");

                // gets the data and writes to table
                final URL url = new URL(TranslationDownloadActivity.BASE_URL
                        + URLEncoder.encode(translationToDownload.shortName, "UTF-8") + ".zip");
                final HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
                final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(httpConnection.getInputStream()));

                final byte buffer[] = new byte[BUFFER_LENGTH];
                final ContentValues versesValues = new ContentValues(4);
                int read = -1;
                int downloaded = 0;
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (isCancelled()) {
                        m_hasError = false;
                        db.endTransaction();
                        db.close();
                    }

                    final ByteArrayOutputStream os = new ByteArrayOutputStream();
                    while ((read = zis.read(buffer, 0, BUFFER_LENGTH)) != -1)
                        os.write(buffer, 0, read);
                    final byte[] bytes = os.toByteArray();

                    String fileName = entry.getName();
                    fileName = fileName.substring(0, fileName.length() - 5); // removes the trailing ".json"
                    if (fileName.equals("books")) {
                        // writes the book names table
                        final ContentValues bookNamesValues = new ContentValues(3);
                        bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME,
                                translationToDownload.shortName);

                        final JSONObject booksInfoObject = new JSONObject(new String(bytes, "UTF8"));
                        final JSONArray booksArray = booksInfoObject.getJSONArray("books");
                        for (int i = 0; i < 66; ++i) { // TODO gets rid of magic number
                            bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, i);
                            bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_NAME, booksArray.getString(i));
                            db.insert(TranslationsDatabaseHelper.TABLE_BOOK_NAMES, null, bookNamesValues);
                        }
                    } else {
                        // writes the verses
                        final String[] parts = fileName.split("-");
                        final int bookIndex = Integer.parseInt(parts[0]);
                        final int chapterIndex = Integer.parseInt(parts[1]);
                        versesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, bookIndex);
                        versesValues.put(TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX, chapterIndex);

                        final JSONObject jsonObject = new JSONObject(new String(bytes, "UTF8"));
                        final JSONArray paragraphArray = jsonObject.getJSONArray("verses");
                        final int paragraphCount = paragraphArray.length();
                        for (int verseIndex = 0; verseIndex < paragraphCount; ++verseIndex) {
                            versesValues.put(TranslationsDatabaseHelper.COLUMN_VERSE_INDEX, verseIndex);
                            versesValues.put(TranslationsDatabaseHelper.COLUMN_TEXT,
                                    paragraphArray.getString(verseIndex));
                            db.insert(translationToDownload.shortName, null, versesValues);
                        }
                    }

                    // notifies the progress
                    updateProgress(++downloaded / 12);
                }
                zis.close();

                // sets as installed
                final ContentValues translationInfoValues = new ContentValues(1);
                translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_INSTALLED, 1);
                db.update(TranslationsDatabaseHelper.TABLE_TRANSLATIONS, translationInfoValues,
                        TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME + " = ?",
                        new String[] { translationToDownload.shortName });

                m_hasError = false;
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
                m_hasError = true;
            } finally {
                db.endTransaction();
                db.close();
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

            m_progressDialog.dismiss();
        }

        protected void onPostExecute(Void result)
        {
            // running in the main thread

            m_progressDialog.dismiss();

            if (m_hasError) {
                Toast.makeText(TranslationDownloadActivity.this, R.string.text_fail_to_fetch_translation,
                        Toast.LENGTH_SHORT).show();
            } else {
                TranslationDownloadActivity.this.finish();
            }
        }

        private static final int BUFFER_LENGTH = 2048;

        private boolean m_hasError;
        private ProgressDialog m_progressDialog;
    }

    private class TranslationListAdapter extends ListBaseAdapter
    {
        public TranslationListAdapter(Context context)
        {
            super(context);
        }

        public void setTexts(String[] texts, int[] sizes)
        {
            m_texts = texts;

            final String size = m_context.getResources().getText(R.string.text_size).toString();
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
                linearLayout = new LinearLayout(m_context);
                linearLayout.setOrientation(LinearLayout.VERTICAL);

                // first line
                TextView textView = new TextView(m_context);
                textView.setGravity(Gravity.CENTER_VERTICAL);
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
            textView.setTextColor(TranslationDownloadActivity.this.m_textColor);
            textView.setText(m_texts[position]);

            // second line
            textView = (TextView) linearLayout.getChildAt(1);
            textView.setText(m_subTexts[position]);

            return linearLayout;
        }

        private String[] m_subTexts;
    }

    protected static final String BASE_URL = "http://bible.zionsoft.net/translations/";

    private int m_textColor;
    private ListView m_translationListView;
    private TranslationDownloadAsyncTask m_translationDownloadAsyncTask;
    private TranslationListAdapter m_translationListAdapter;
    private TranslationManager m_translationManager;
    private TranslationInfo[] m_availableTranslations;
}
