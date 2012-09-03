package net.zionsoft.obadiah;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
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

        BibleReader bibleReader = BibleReader.getInstance();
        // NOTE must call this before BibleReader is further used
        bibleReader.setRootDir(getFilesDir());

        // loads the translation as used last time
        final SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        String selectedTranslation = null;
        try {
            selectedTranslation = preferences.getString("selectedTranslation", null);
        } catch (ClassCastException e) {
            // the value is an integer for 1.0 and 1.1
            int selected = preferences.getInt("selectedTranslation", -1);
            if (selected == 0)
                selectedTranslation = "authorized-king-james";
            else if (selected == 1)
                selectedTranslation = "chinese-union-simplified";
        }
        bibleReader.selectTranslation(selectedTranslation);

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
                m_needToUpdateLastRead = true;

                Intent intent = new Intent(BookSelectionActivity.this, TextActivity.class);
                intent.putExtra("selectedBook", m_selectedBook);
                intent.putExtra("selectedChapter", position);
                if ((m_lastReadBook == m_selectedBook) && (m_lastReadChapter == position))
                    intent.putExtra("selectedVerse", m_lastReadVerse);
                else
                    intent.putExtra("selectedVerse", 0);
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
    }

    protected void onResume()
    {
        super.onResume();

        // opens dialog if no translation installed
        // the code is here in case the user doesn't download anything
        final TranslationInfo[] installedTranslations = BibleReader.getInstance().installedTranslations();
        if (installedTranslations == null || installedTranslations.length == 0) {
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
            return;
        }

        // loads the last read book and chapter if from TextActivity or first boot
        // i.e. not from TranslationSelectionActivity
        if (m_needToUpdateLastRead) {
            final SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
            m_lastReadBook = preferences.getInt("currentBook", -1);
            m_lastReadChapter = preferences.getInt("currentChapter", -1);
            m_lastReadVerse = preferences.getInt("currentVerse", -1);

            if (m_startup)
                m_selectedBook = m_lastReadBook;
        }

        // sets the chapter lists if from TextActivity or TranslationSelectionActivity with 1st download translation
        if (m_needToUpdateLastRead || m_selectedBook < 0) {
            if (m_selectedBook < 0)
                m_selectedBook = 0;
            updateChapterSelectionListAdapter();

            m_needToUpdateLastRead = false;
        }

        // updates the title and texts
        // TODO no need to update if selected translation is not changed
        TranslationInfo translationInfo = BibleReader.getInstance().selectedTranslation();
        if (translationInfo == null)
            return;
        m_titleTranslationTextView.setText(translationInfo.shortName);
        m_bookSelectionListAdapter.setTexts(translationInfo.bookName);

        if (m_startup) {
            m_bookListView.setSelection(m_selectedBook);
            m_startup = false;
        }
    }

    private void updateChapterSelectionListAdapter()
    {
        final int chapterCount = BibleReader.getInstance().chapterCount(m_selectedBook);
        String[] chapters = new String[chapterCount];
        for (int i = 0; i < chapterCount; ++i)
            chapters[i] = Integer.toString(i + 1);
        m_chapterSelectionListAdapter.setTexts(chapters);
    }

    private void startTranslationSelectionActivity()
    {
        m_needToUpdateLastRead = false;

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
                textView.setBackgroundResource(R.drawable.book_list_item_background);
                textView.setTextColor(Color.BLACK);
            } else {
                textView = (TextView) convertView;
            }

            if (m_selectedBook == position) {
                textView.setTypeface(null, Typeface.BOLD);
                textView.setBackgroundResource(R.drawable.book_list_item_background_selected);
            } else {
                textView.setTypeface(null, Typeface.NORMAL);
                textView.setBackgroundResource(R.drawable.book_list_item_background);
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
                textView.setBackgroundResource(R.drawable.book_list_item_background);
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

    private boolean m_startup = true;
    private boolean m_needToUpdateLastRead = true;
    private int m_lastReadBook = -1;
    private int m_lastReadChapter = -1;
    private int m_lastReadVerse = -1;
    private int m_selectedBook = -1;
    private BookSelectionListAdapter m_bookSelectionListAdapter;
    private ChapterSelectionListAdapter m_chapterSelectionListAdapter;
    private ListView m_bookListView;
    private GridView m_chapterGridView;
    private TextView m_titleTranslationTextView;
}
