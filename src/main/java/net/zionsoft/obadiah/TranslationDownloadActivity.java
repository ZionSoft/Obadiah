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
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.bible.TranslationManager;
import net.zionsoft.obadiah.bible.TranslationsDatabaseHelper;
import net.zionsoft.obadiah.util.NetworkHelper;
import net.zionsoft.obadiah.util.SettingsManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TranslationDownloadActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.translationdownload_activity);

        mSettingsManager = new SettingsManager(this);
        mTranslationManager = new TranslationManager(this);

        // initializes views
        mLoadingSpinner = findViewById(R.id.translation_download_loading_spinner);

        // initializes list view showing available translations
        mTranslationListView = (ListView) findViewById(R.id.translation_listview);
        mTranslationListAdapter = new TranslationDownloadListAdapter(this, mSettingsManager);
        mTranslationListView.setAdapter(mTranslationListAdapter);
        mTranslationListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TranslationDownloadActivity.this.mTranslationDownloadAsyncTask = new TranslationDownloadAsyncTask();
                TranslationDownloadActivity.this.mTranslationDownloadAsyncTask.execute(position);
            }
        });

        loadTranslationList(false);
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
                loadTranslationList(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // loads translation list

    private void loadTranslationList(final boolean forceRefresh) {
        if (NetworkHelper.hasNetworkConnection(this)) {
            new TranslationListDownloadAsyncTask().execute(forceRefresh);
        } else {
            DialogHelper.showDialog(this, R.string.dialog_no_network_message,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            loadTranslationList(forceRefresh);
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    }
            );
        }
    }

    private class TranslationListDownloadAsyncTask extends AsyncTask<Boolean, Void, List<TranslationInfo>> {
        protected void onPreExecute() {
            // running in the main thread

            mLoadingSpinner.setVisibility(View.VISIBLE);
            mTranslationListView.setVisibility(View.GONE);
        }

        protected List<TranslationInfo> doInBackground(Boolean... params) {
            // running in the worker thread

            try {
                boolean forceRefresh = params[0];
                List<TranslationInfo> translations = null;
                if (!forceRefresh) {
                    translations = mTranslationManager.availableTranslations();
                    if (translations.size() == 0)
                        forceRefresh = true;
                }

                if (forceRefresh) {
                    mTranslationManager.addTranslations(NetworkHelper.fetchTranslationList());
                    translations = mTranslationManager.availableTranslations();
                }
                return translations;
            } catch (Exception e) {
                return null;
            }
        }

        protected void onPostExecute(List<TranslationInfo> translations) {
            // running in the main thread

            Animator.fadeOut(mLoadingSpinner);
            Animator.fadeIn(mTranslationListView);

            if (translations == null) {
                // error occurs
                DialogHelper.showDialog(TranslationDownloadActivity.this,
                        R.string.dialog_translation_list_fetch_failure_message,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                loadTranslationList(true);
                            }
                        },
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        }
                );
            } else if (translations.size() == 0) {
                // no translations available
                Toast.makeText(TranslationDownloadActivity.this,
                        R.string.text_no_available_translation, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                // all is well
                mAvailableTranslations = translations;
                mTranslationListAdapter.setTranslations(mAvailableTranslations);
            }
        }
    }


    // downloads translation

    protected class TranslationDownloadAsyncTask extends AsyncTask<Integer, Integer, Void> {
        public void updateProgress(int progress) {
            publishProgress(progress);
        }

        protected void onPreExecute() {
            // running in the main thread

            mProgressDialog = new ProgressDialog(TranslationDownloadActivity.this);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    TranslationDownloadAsyncTask.this.cancel(true);
                }
            });
            mProgressDialog.setMessage(TranslationDownloadActivity.this.getText(R.string.text_downloading));
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(0);
            mProgressDialog.show();
        }

        protected Void doInBackground(Integer... positions) {
            // running in the worker thread

            // the logic should be in TranslationManager

            final TranslationInfo translationToDownload = TranslationDownloadActivity.this.mAvailableTranslations.get(positions[0]);
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
                        mHasError = false;
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

                mHasError = false;
                db.setTransactionSuccessful();
            } catch (Exception e) {
                e.printStackTrace();
                mHasError = true;
            } finally {
                db.endTransaction();
                db.close();
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
            // running in the main thread

            mProgressDialog.setProgress(progress[0]);
        }

        protected void onCancelled() {
            // running in the main thread

            mProgressDialog.dismiss();
        }

        protected void onPostExecute(Void result) {
            // running in the main thread

            mProgressDialog.dismiss();

            if (mHasError) {
                Toast.makeText(TranslationDownloadActivity.this, R.string.text_fail_to_fetch_translation,
                        Toast.LENGTH_SHORT).show();
            } else {
                TranslationDownloadActivity.this.finish();
            }
        }

        private static final int BUFFER_LENGTH = 2048;

        private boolean mHasError;
        private ProgressDialog mProgressDialog;
    }

    protected static final String BASE_URL = "http://bible.zionsoft.net/translations/";

    private ListView mTranslationListView;
    private View mLoadingSpinner;

    private SettingsManager mSettingsManager;
    private TranslationDownloadAsyncTask mTranslationDownloadAsyncTask;
    private TranslationDownloadListAdapter mTranslationListAdapter;
    private TranslationManager mTranslationManager;
    private List<TranslationInfo> mAvailableTranslations;
}
