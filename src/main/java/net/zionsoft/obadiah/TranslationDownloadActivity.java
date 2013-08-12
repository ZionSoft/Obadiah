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
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.bible.TranslationManager;
import net.zionsoft.obadiah.bible.TranslationsDatabaseHelper;
import net.zionsoft.obadiah.util.SettingsManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TranslationDownloadActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.translationdownload_activity);

        mSettingsManager = new SettingsManager(this);
        mTranslationManager = new TranslationManager(this);

        // initializes list view showing available translations
        mTranslationListView = (ListView) findViewById(R.id.translation_listview);
        mTranslationListAdapter = new TranslationListAdapter(this);
        mTranslationListView.setAdapter(mTranslationListAdapter);
        mTranslationListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TranslationDownloadActivity.this.mTranslationDownloadAsyncTask = new TranslationDownloadAsyncTask();
                TranslationDownloadActivity.this.mTranslationDownloadAsyncTask.execute(position);
            }
        });

        // gets translation list
        new TranslationListDownloadAsyncTask().execute(false);
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
    protected void onPause() {
        super.onPause();

        // cancel existing download async tasks
        if (mTranslationDownloadAsyncTask != null)
            mTranslationDownloadAsyncTask.cancel(true);
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
                new TranslationListDownloadAsyncTask().execute(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class TranslationListDownloadAsyncTask extends AsyncTask<Boolean, Void, Void> {
        protected void onPreExecute() {
            // running in the main thread

            m_progressDialog = new ProgressDialog(TranslationDownloadActivity.this);
            m_progressDialog.setCancelable(false);
            m_progressDialog.setMessage(TranslationDownloadActivity.this.getText(R.string.text_downloading));
            m_progressDialog.show();
        }

        protected Void doInBackground(Boolean... params) {
            // running in the worker thread

            try {
                final long lastUpdated = TranslationDownloadActivity.this.getSharedPreferences(Constants.SETTING_KEY,
                        MODE_PRIVATE).getLong(Constants.LAST_UPDATED_SETTING_KEY, 0);
                final long now = System.currentTimeMillis();
                if (params[0] || lastUpdated <= 0 || lastUpdated >= now || ((now - lastUpdated) >= 86400000)) {
                    // force update
                    // or no valid local cache, i.e. not fetched before, or last fetched more than 1 day ago

                    // downloads from Internet
                    final URL url = new URL(String.format("%slist.json", BASE_URL));
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
                    TranslationDownloadActivity.this.mTranslationManager.addTranslations(allTranslations);
                }

                // sets the available translations
                final TranslationInfo[] allTranslations = TranslationDownloadActivity.this.mTranslationManager
                        .translations();
                int availableTranslationsCount = 0;
                for (TranslationInfo translationInfo : allTranslations) {
                    if (!translationInfo.installed)
                        ++availableTranslationsCount;
                }
                if (availableTranslationsCount == 0) {
                    TranslationDownloadActivity.this.mAvailableTranslations = null;
                } else if (availableTranslationsCount == allTranslations.length) {
                    TranslationDownloadActivity.this.mAvailableTranslations = allTranslations;
                } else {
                    final TranslationInfo[] availableTranslations = new TranslationInfo[availableTranslationsCount];
                    int index = 0;
                    for (TranslationInfo translationInfo : allTranslations) {
                        if (!translationInfo.installed)
                            availableTranslations[index++] = translationInfo;
                    }
                    TranslationDownloadActivity.this.mAvailableTranslations = availableTranslations;
                }
            } catch (Exception e) {
                e.printStackTrace();

                m_hasError = true;
                TranslationDownloadActivity.this.mAvailableTranslations = null;
            } finally {
                final SharedPreferences.Editor editor = TranslationDownloadActivity.this.getSharedPreferences(
                        Constants.SETTING_KEY, MODE_PRIVATE).edit();
                if (m_hasError || TranslationDownloadActivity.this.mAvailableTranslations == null)
                    editor.putLong(Constants.LAST_UPDATED_SETTING_KEY, 0);
                else
                    editor.putLong(Constants.LAST_UPDATED_SETTING_KEY, System.currentTimeMillis());
                editor.commit();
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            // running in the main thread

            m_progressDialog.dismiss();

            if (m_hasError || TranslationDownloadActivity.this.mAvailableTranslations == null
                    || TranslationDownloadActivity.this.mAvailableTranslations.length == 0) {
                // either error occurs, or no available translations
                Toast.makeText(
                        TranslationDownloadActivity.this,
                        m_hasError ? R.string.text_fail_to_fetch_translations_list
                                : R.string.text_no_available_translation, Toast.LENGTH_SHORT).show();
                TranslationDownloadActivity.this.finish();
            } else {
                // everything fine with available translations
                final int length = TranslationDownloadActivity.this.mAvailableTranslations.length;
                final String[] texts = new String[length];
                final int[] sizes = new int[length];
                for (int i = 0; i < length; ++i) {
                    texts[i] = TranslationDownloadActivity.this.mAvailableTranslations[i].name;
                    sizes[i] = TranslationDownloadActivity.this.mAvailableTranslations[i].size;
                }
                TranslationDownloadActivity.this.mTranslationListAdapter.setTexts(texts, sizes);
            }
        }

        private boolean m_hasError;
        private ProgressDialog m_progressDialog;
    }

    protected class TranslationDownloadAsyncTask extends AsyncTask<Integer, Integer, Void> {
        public void updateProgress(int progress) {
            publishProgress(progress);
        }

        protected void onPreExecute() {
            // running in the main thread

            m_progressDialog = new ProgressDialog(TranslationDownloadActivity.this);
            m_progressDialog.setCancelable(true);
            m_progressDialog.setCanceledOnTouchOutside(false);
            m_progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    TranslationDownloadAsyncTask.this.cancel(true);
                }
            });
            m_progressDialog.setMessage(TranslationDownloadActivity.this.getText(R.string.text_downloading));
            m_progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            m_progressDialog.setMax(100);
            m_progressDialog.setProgress(0);
            m_progressDialog.show();
        }

        protected Void doInBackground(Integer... positions) {
            // running in the worker thread

            // the logic should be in TranslationManager

            final TranslationInfo translationToDownload = TranslationDownloadActivity.this.mAvailableTranslations[positions[0]];
            final SQLiteDatabase db = new TranslationsDatabaseHelper(TranslationDownloadActivity.this)
                    .getWritableDatabase();
            db.beginTransaction();
            try {
                // creates a translation table
                db.execSQL(String.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL);",
                        translationToDownload.shortName,
                        TranslationsDatabaseHelper.COLUMN_BOOK_INDEX,
                        TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX,
                        TranslationsDatabaseHelper.COLUMN_VERSE_INDEX,
                        TranslationsDatabaseHelper.COLUMN_TEXT));
                db.execSQL(String.format("CREATE INDEX INDEX_%s ON %s (%s, %s, %s);",
                        translationToDownload.shortName, translationToDownload.shortName,
                        TranslationsDatabaseHelper.COLUMN_BOOK_INDEX,
                        TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX,
                        TranslationsDatabaseHelper.COLUMN_VERSE_INDEX));

                // gets the data and writes to table
                final URL url = new URL(String.format("%s%s.zip",
                        TranslationDownloadActivity.BASE_URL,
                        URLEncoder.encode(translationToDownload.shortName, "UTF-8")));
                final HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
                final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(httpConnection.getInputStream()));

                final byte buffer[] = new byte[BUFFER_LENGTH];
                final ContentValues versesValues = new ContentValues(4);
                int read;
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
                        String.format("%s = ?",
                                TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME),
                        new String[]{translationToDownload.shortName});

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

        protected void onProgressUpdate(Integer... progress) {
            // running in the main thread

            m_progressDialog.setProgress(progress[0]);
        }

        protected void onCancelled() {
            // running in the main thread

            m_progressDialog.dismiss();
        }

        protected void onPostExecute(Void result) {
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

    private class TranslationListAdapter extends ListBaseAdapter {
        public TranslationListAdapter(Context context) {
            super(context);
            Resources resources = mContext.getResources();
            m_mediumSizeSpan = new AbsoluteSizeSpan(resources.getDimensionPixelSize(R.dimen.text_size_medium));
            m_smallSizeSpan = new AbsoluteSizeSpan(resources.getDimensionPixelSize(R.dimen.text_size_small));
        }

        public void setTexts(String[] texts, int[] sizes) {
            mTexts = texts;
            mSizes = sizes;

            notifyDataSetChanged();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                textView = (TextView) View.inflate(mContext,
                        R.layout.translation_download_list_item, null);
            } else {
                textView = (TextView) convertView;
            }

            String string
                    = mContext.getResources().getString(R.string.text_available_translation_info,
                    mTexts[position], mSizes[position]);
            SpannableStringBuilder spannable = new SpannableStringBuilder(string);
            spannable.setSpan(m_mediumSizeSpan, 0, mTexts[position].length(), 0);
            spannable.setSpan(m_smallSizeSpan, mTexts[position].length(), spannable.length(), 0);
            textView.setText(spannable);

            return textView;
        }

        private AbsoluteSizeSpan m_mediumSizeSpan;
        private AbsoluteSizeSpan m_smallSizeSpan;
        private int[] mSizes;
    }

    protected static final String BASE_URL = "http://bible.zionsoft.net/translations/";

    private ListView mTranslationListView;
    private SettingsManager mSettingsManager;
    private TranslationDownloadAsyncTask mTranslationDownloadAsyncTask;
    private TranslationListAdapter mTranslationListAdapter;
    private TranslationManager mTranslationManager;
    private TranslationInfo[] mAvailableTranslations;
}
