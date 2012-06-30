package net.zionsoft.obadiah;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;

public class BookSelectionActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_gridview);

        File filesDir = getFilesDir();
        final int length = filesDir.list().length;
        // first time use
        if (length == 0)
            new FileCopyAsyncTask().execute(this);

        BibleReader bibleReader = BibleReader.getInstance();
        bibleReader.setRootDir(filesDir);
        bibleReader.selectTranslation(getSharedPreferences("settings", MODE_PRIVATE).getInt("selectedTranslation", 0));

        GridView gridView = (GridView) findViewById(R.id.gridView);
        gridView.setColumnWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 144, getResources()
                .getDisplayMetrics()));
        m_listAdapter = new SelectionListAdapter(this);
        gridView.setAdapter(m_listAdapter);
        gridView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Intent intent = new Intent(BookSelectionActivity.this, ChapterSelectionActivity.class);
                intent.putExtra("continueReading", false);
                intent.putExtra("selectedBook", position);
                startActivity(intent);
            }
        });
    }

    protected void onResume()
    {
        super.onResume();

        TranslationInfo translationInfo = BibleReader.getInstance().selectedTranslation();
        if (translationInfo == null)
            return;
        setTitle(translationInfo.name);
        m_listAdapter.setTexts(translationInfo.bookName);
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_book_selection_activity, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem menuItem = menu.findItem(R.id.menu_continue_reading);
        if (getSharedPreferences("settings", MODE_PRIVATE).getInt("currentBook", -1) >= 0) {
            menuItem.setVisible(true);
        } else {
            menuItem.setVisible(false);
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_continue_reading: {
            Intent intent = new Intent(this, ChapterSelectionActivity.class);
            intent.putExtra("continueReading", true);
            intent.putExtra("selectedBook", getSharedPreferences("settings", MODE_PRIVATE).getInt("currentBook", 0));
            startActivity(intent);
            return true;
        }
        case R.id.menu_select_translation: {
            Intent intent = new Intent(this, TranslationSelectionActivity.class);
            startActivity(intent);
            return true;
        }
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private class FileCopyAsyncTask extends AsyncTask<Activity, Integer, Void>
    {
        protected void onPreExecute()
        {
            // running in the main thread
            m_progressDialog = new ProgressDialog(BookSelectionActivity.this);
            m_progressDialog.setCancelable(false);
            m_progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            m_progressDialog.setMessage(getText(R.string.text_initializing));
            m_progressDialog.setMax(100);
            m_progressDialog.setProgress(0);
            m_progressDialog.show();
        }

        protected Void doInBackground(Activity... activities)
        {
            // running in the worker thread
            try {
                Activity activity = activities[0];
                AssetManager assetManager = activity.getAssets();
                String[] fileList = assetManager.list("bible");
                final int fileCount = fileList.length;

                final int total = 12 * fileCount;
                int unzipped = 0;

                byte[] buffer = new byte[BUFFER_LENGTH];
                int read = -1;
                File filesDir = activity.getFilesDir();
                for (int i = 0; i < fileCount; ++i) {
                    // creates sub-folder
                    String fileName = fileList[i];
                    File dir = new File(filesDir, fileName.substring(0, fileName.length() - 4));
                    dir.mkdir();

                    // writes to internal storage
                    ZipInputStream zis = new ZipInputStream(new BufferedInputStream(assetManager.open("bible/"
                            + fileName)));
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        FileOutputStream fos = new FileOutputStream(new File(dir, entry.getName()));
                        BufferedOutputStream os = new BufferedOutputStream(fos, BUFFER_LENGTH);
                        while ((read = zis.read(buffer, 0, BUFFER_LENGTH)) != -1)
                            os.write(buffer, 0, read);
                        os.flush();
                        os.close();

                        // notifies the progress
                        publishProgress(++unzipped / total);
                    }
                    zis.close();
                }
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

        protected void onPostExecute(Void result)
        {
            // running in the main thread
            BibleReader bibleReader = BibleReader.getInstance();
            bibleReader.refresh();
            bibleReader.selectTranslation(0);
            TranslationInfo translationInfo = bibleReader.selectedTranslation();
            if (translationInfo != null) {
                setTitle(translationInfo.name);
                m_listAdapter.setTexts(translationInfo.bookName);
            }

            m_progressDialog.dismiss();
        }

        private static final int BUFFER_LENGTH = 2048;
        private ProgressDialog m_progressDialog;
    }

    private SelectionListAdapter m_listAdapter;
}
