package net.zionsoft.obadiah;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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

        // convert to new format from old format (used prior to 1.5.0) if needed
        if (getFilesDir().list().length > 0) {
            m_converting = true;
            new TranslationsConvertionAsyncTask().execute();
        }

        m_translationManager = new TranslationManager(this);
        m_translationReader = new TranslationReader(this);

        // initializes the title bar
        m_selectedTranslationTextView = (TextView) findViewById(R.id.textTranslationSelection);
        m_selectedTranslationTextView.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                BookSelectionActivity.this.startTranslationSelectionActivity();
            }
        });
        m_selectedBookTextView = (TextView) findViewById(R.id.textBookName);

        // initializes the book names list view
        m_bookListView = (ListView) findViewById(R.id.bookListView);
        m_bookListAdapter = new BookListAdapter(this);
        m_bookListView.setAdapter(m_bookListAdapter);
        m_bookListView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (BookSelectionActivity.this.m_selectedBook == position)
                    return;

                BookSelectionActivity.this.m_selectedBook = position;
                BookSelectionActivity.this.m_selectedBookTextView.setText(m_translationReader.bookNames()[m_selectedBook]);
                BookSelectionActivity.this.m_bookListAdapter.notifyDataSetChanged();
                BookSelectionActivity.this.updateChapterSelectionListAdapter();
            }
        });

        // initializes the chapters selection grid view
        final GridView chaptersGridView = (GridView) findViewById(R.id.chapterGridView);
        m_chapterListAdapter = new ChapterListAdapter(this);
        chaptersGridView.setAdapter(m_chapterListAdapter);
        chaptersGridView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                final SharedPreferences preferences = BookSelectionActivity.this.getSharedPreferences("settings",
                        MODE_PRIVATE);
                if (preferences.getInt("currentBook", -1) != m_selectedBook
                        || preferences.getInt("currentChapter", -1) != position) {
                    final SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("currentBook", m_selectedBook);
                    editor.putInt("currentChapter", position);
                    editor.putInt("currentVerse", 0);
                    editor.commit();
                }

                startActivity(new Intent(BookSelectionActivity.this, TextActivity.class));
            }
        });
    }

    protected void onResume()
    {
        super.onResume();

        if (m_converting)
            return;

        final TranslationInfo[] translations = m_translationManager.translations();
        if (translations != null) {
            for (TranslationInfo translationInfo : translations) {
                if (translationInfo.installed) {
                    populateUi();
                    return;
                }
            }
        }

        // no translation is installed
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(R.string.text_no_translation).setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.dismiss();
                        BookSelectionActivity.this.startTranslationSelectionActivity();
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.cancel();
                        BookSelectionActivity.this.finish();
                    }
                });
        alertDialogBuilder.create().show();
    }

    public void search(View view)
    {
        startActivity(new Intent(this, SearchActivity.class));
    }

    private void populateUi()
    {
        // loads last used translation
        final SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        String selectedTranslation = null;
        try {
            selectedTranslation = preferences.getString("selectedTranslation", null);
        } catch (ClassCastException e) {
            // the value is an integer prior to 1.2.0
            final int selected = preferences.getInt("selectedTranslation", 0);
            if (selected == 1)
                selectedTranslation = "chinese-union-simplified";
            else
                selectedTranslation = "authorized-king-james";
        }
        m_translationReader.selectTranslation(selectedTranslation);

        // loads the last read book and chapter
        m_currentBook = preferences.getInt("currentBook", -1);
        m_currentChapter = preferences.getInt("currentChapter", -1);

        // sets the book that is currently selected
        m_selectedBook = m_currentBook < 0 ? 0 : m_currentBook;

        // sets the chapter lists
        // TODO it's not needed if this activity is resumed from TranslationSelectionActivity
        updateChapterSelectionListAdapter();

        // updates the title and book names
        // TODO no need to update if selected translation is not changed
        m_selectedTranslationTextView.setText(m_translationReader.selectedTranslationShortName());
        final String[] bookNames = m_translationReader.bookNames();
        m_selectedBookTextView.setText(bookNames[m_selectedBook]);
        m_bookListAdapter.setTexts(bookNames);

        // scrolls to the currently selected book
        m_bookListView.setSelection(m_selectedBook);
    }

    private void updateChapterSelectionListAdapter()
    {
        final int chapterCount = TranslationReader.chapterCount(m_selectedBook);
        final String[] chapters = new String[chapterCount];
        for (int i = 0; i < chapterCount; ++i)
            chapters[i] = Integer.toString(i + 1);
        m_chapterListAdapter.setTexts(chapters);
    }

    private void startTranslationSelectionActivity()
    {
        startActivity(new Intent(this, TranslationSelectionActivity.class));
    }

    private class BookListAdapter extends ListBaseAdapter
    {
        public BookListAdapter(Context context)
        {
            super(context);

            m_textViewHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 47, context.getResources()
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

            if (BookSelectionActivity.this.m_selectedBook == position) {
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

    public class ChapterListAdapter extends ListBaseAdapter
    {
        public ChapterListAdapter(Context context)
        {
            super(context);

            m_textViewHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 37, context.getResources()
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

            if (m_currentBook == m_selectedBook && m_currentChapter == position)
                textView.setTypeface(null, Typeface.BOLD);
            else
                textView.setTypeface(null, Typeface.NORMAL);
            textView.setText(m_texts[position]);
            return textView;
        }

        private int m_textViewHeight;
    }

    private class TranslationsConvertionAsyncTask extends AsyncTask<Void, Void, Void>
    {
        protected void onPreExecute()
        {
            // running in the main thread

            BookSelectionActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

            m_progressDialog = new ProgressDialog(BookSelectionActivity.this);
            m_progressDialog.setCancelable(false);
            m_progressDialog.setMessage(BookSelectionActivity.this.getText(R.string.text_initializing));
            m_progressDialog.show();
        }

        protected Void doInBackground(Void... params)
        {
            // running in the worker thread

            BookSelectionActivity.this.m_translationManager.convertFromOldFormat();
            Utils.removeDirectory(BookSelectionActivity.this.getFilesDir());
            return null;
        }

        protected void onPostExecute(Void result)
        {
            // running in the main thread

            BookSelectionActivity.this.populateUi();
            m_progressDialog.dismiss();
            BookSelectionActivity.this.m_converting = false;

            BookSelectionActivity.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        private ProgressDialog m_progressDialog;
    }

    private boolean m_converting = false;
    private int m_currentBook = -1;
    private int m_currentChapter = -1;
    private int m_selectedBook = -1;
    private BookListAdapter m_bookListAdapter;
    private ChapterListAdapter m_chapterListAdapter;
    private ListView m_bookListView;
    private TextView m_selectedBookTextView;
    private TextView m_selectedTranslationTextView;
    private TranslationManager m_translationManager;
    private TranslationReader m_translationReader;
}
