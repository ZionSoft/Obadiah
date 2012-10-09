package net.zionsoft.obadiah;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class BookSelectionActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bookselection_activity);

        m_translationManager = new TranslationManager(this);
        m_translationReader = new TranslationReader(this);

        // convert to new format (old format used prior to 1.5.0) if needed
        if (getFilesDir().list().length > 0) {
            m_converting = true;
            new TranslationsConvertionAsyncTask(m_translationManager, this).execute();
        }

        // initializes the view for book names
        m_bookListView = (ListView) findViewById(R.id.bookListView);
        m_bookSelectionListAdapter = new BookSelectionListAdapter(this);
        m_bookListView.setAdapter(m_bookSelectionListAdapter);
        m_bookListView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (m_selectedBook == position)
                    return;

                m_selectedBook = position;
                m_titleBookNameTextView.setText(m_translationReader.bookNames()[m_selectedBook]);
                m_bookSelectionListAdapter.notifyDataSetChanged();
                updateChapterSelectionListAdapter();
            }
        });

        // initializes the view for chapters
        m_chapterGridView = (GridView) findViewById(R.id.chapterGridView);
        m_chapterSelectionListAdapter = new ChapterSelectionListAdapter(this);
        m_chapterGridView.setAdapter(m_chapterSelectionListAdapter);
        m_chapterGridView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Intent intent = new Intent(BookSelectionActivity.this, TextActivity.class);
                intent.putExtra("selectedBook", m_selectedBook);
                intent.putExtra("selectedChapter", position);
                if ((m_lastReadBook == m_selectedBook) && (m_lastReadChapter == position)) {
                    intent.putExtra("selectedVerse",
                            getSharedPreferences("settings", MODE_PRIVATE).getInt("currentVerse", -1));
                } else {
                    intent.putExtra("selectedVerse", 0);
                }
                startActivity(intent);
            }
        });

        // initializes the title bar
        m_titleTranslationTextView = (TextView) findViewById(R.id.textTranslationSelection);
        m_titleTranslationTextView.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                startTranslationSelectionActivity();
            }
        });
        m_titleBookNameTextView = (TextView) findViewById(R.id.textBookName);
    }

    protected void onResume()
    {
        super.onResume();

        if (m_converting)
            return;

        final TranslationInfo[] translations = m_translationManager.translations();
        if (translations == null) {
            showNoTranslationDialog();
            return;
        }
        for (TranslationInfo translationInfo : translations) {
            if (translationInfo.installed) {
                populateUi();
                return;
            }
        }
        showNoTranslationDialog();
    }

    private void showNoTranslationDialog()
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(R.string.text_no_translation).setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.dismiss();
                        startTranslationSelectionActivity();
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.cancel();
                        BookSelectionActivity.this.finish();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void populateUi()
    {
        // loads the translation as used last time
        final SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        String selectedTranslation = null;
        try {
            selectedTranslation = preferences.getString("selectedTranslation", null);
        } catch (ClassCastException e) {
            // the value is an integer prior to 1.2.0
            int selected = preferences.getInt("selectedTranslation", -1);
            if (selected == 0)
                selectedTranslation = "authorized-king-james";
            else if (selected == 1)
                selectedTranslation = "chinese-union-simplified";
        }
        m_translationReader.selectTranslation(selectedTranslation);

        // loads the last read book and chapter
        m_lastReadBook = preferences.getInt("currentBook", -1);
        m_lastReadChapter = preferences.getInt("currentChapter", -1);

        // sets the book that is currently selected
        if (m_selectedBook < 0)
            m_selectedBook = m_lastReadBook < 0 ? 0 : m_lastReadBook;

        // sets the chapter lists
        // TODO it's not needed if this activity is resumed from TranslationSelectionActivity
        updateChapterSelectionListAdapter();

        // updates the title and book names
        // TODO no need to update if selected translation is not changed
        m_titleTranslationTextView.setText(m_translationReader.selectedTranslationShortName());
        m_titleBookNameTextView.setText(m_translationReader.bookNames()[m_selectedBook]);
        m_bookSelectionListAdapter.setTexts(m_translationReader.bookNames());

        m_bookListView.setSelection(m_selectedBook);
    }

    private void updateChapterSelectionListAdapter()
    {
        final int chapterCount = m_translationReader.chapterCount(m_selectedBook);
        String[] chapters = new String[chapterCount];
        for (int i = 0; i < chapterCount; ++i)
            chapters[i] = Integer.toString(i + 1);
        m_chapterSelectionListAdapter.setTexts(chapters);
    }

    private void startTranslationSelectionActivity()
    {
        Intent intent = new Intent(this, TranslationSelectionActivity.class);
        startActivity(intent);
    }

    private class BookSelectionListAdapter extends ListBaseAdapter
    {
        public BookSelectionListAdapter(Context context)
        {
            super(context);

            m_textViewHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 47, getResources()
                    .getDisplayMetrics());
        }

        public void setTexts(String[] texts)
        {
            m_texts = texts;
            notifyDataSetChanged();
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            TextView textView;
            if (convertView == null) {
                textView = new TextView(m_context);
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
                textView.setHeight(m_textViewHeight);
                textView.setBackgroundResource(R.drawable.list_item_background);
                textView.setTextColor(Color.BLACK);
            } else {
                textView = (TextView) convertView;
            }

            if (m_selectedBook == position) {
                textView.setTypeface(null, Typeface.BOLD);
                textView.setBackgroundResource(R.drawable.list_item_background_selected);
            } else {
                textView.setTypeface(null, Typeface.NORMAL);
                textView.setBackgroundResource(R.drawable.list_item_background);
            }
            textView.setText(m_texts[position]);
            return textView;
        }

        private int m_textViewHeight;
    }

    public class ChapterSelectionListAdapter extends ListBaseAdapter
    {
        public ChapterSelectionListAdapter(Context context)
        {
            super(context);

            m_textViewHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 37, getResources()
                    .getDisplayMetrics());
        }

        public void setTexts(String[] texts)
        {
            m_texts = texts;
            notifyDataSetChanged();
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            TextView textView;
            if (convertView == null) {
                textView = new TextView(m_context);
                textView.setBackgroundResource(R.drawable.list_item_background);
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
                textView.setHeight(m_textViewHeight);
                textView.setTextColor(Color.BLACK);
            } else {
                textView = (TextView) convertView;
            }

            if (m_lastReadBook == m_selectedBook && m_lastReadChapter == position)
                textView.setTypeface(null, Typeface.BOLD);
            else
                textView.setTypeface(null, Typeface.NORMAL);
            textView.setText(m_texts[position]);
            return textView;
        }

        private int m_textViewHeight;
    }

    private static class TranslationsConvertionAsyncTask extends AsyncTask<Void, Void, Void>
    {
        public TranslationsConvertionAsyncTask(TranslationManager translationManager, BookSelectionActivity activity)
        {
            super();
            m_bookSelectionActivity = activity;
            m_translationManager = translationManager;
        }

        protected void onPreExecute()
        {
            // running in the main thread
            m_progressDialog = new ProgressDialog(m_bookSelectionActivity);
            m_progressDialog.setCancelable(false);
            m_progressDialog.setMessage(m_bookSelectionActivity.getText(R.string.text_initializing));
            m_progressDialog.show();
        }

        protected Void doInBackground(Void... params)
        {
            // running in the worker thread
            final File root = m_bookSelectionActivity.getFilesDir();
            BibleReader.getInstance().setRootDir(root);
            m_translationManager.convertFromOldFormat();
            Utils.removeDirectory(root);
            return null;
        }

        protected void onPostExecute(Void result)
        {
            // running in the main thread
            m_bookSelectionActivity.populateUi();
            m_progressDialog.dismiss();
            m_bookSelectionActivity.m_converting = false;
        }

        private BookSelectionActivity m_bookSelectionActivity;
        private TranslationManager m_translationManager;
        private ProgressDialog m_progressDialog;
    }

    private boolean m_converting = false;
    private int m_lastReadBook = -1;
    private int m_lastReadChapter = -1;
    private int m_selectedBook = -1;
    private BookSelectionListAdapter m_bookSelectionListAdapter;
    private ChapterSelectionListAdapter m_chapterSelectionListAdapter;
    private ListView m_bookListView;
    private GridView m_chapterGridView;
    private TextView m_titleBookNameTextView;
    private TextView m_titleTranslationTextView;
    private TranslationManager m_translationManager;
    private TranslationReader m_translationReader;
}
