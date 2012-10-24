package net.zionsoft.obadiah;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

public class SearchActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        m_translationsDatabaseHelper = new TranslationsDatabaseHelper(this);
        m_translationManager = new TranslationManager(this);
        m_translationReader = new TranslationReader(this);

        // initializes the title bar
        m_selectedTranslationTextView = (TextView) findViewById(R.id.textTranslationSelection);
        m_selectedTranslationTextView.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                startActivity(new Intent(SearchActivity.this, TranslationSelectionActivity.class));
            }
        });

        // initializes the search bar
        m_searchText = (EditText) findViewById(R.id.searchText);
        m_searchText.setOnEditorActionListener(new OnEditorActionListener()
        {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    SearchActivity.this.search(null);
                    return true;
                }
                return false;
            }
        });

        // initializes the search results list view
        m_searchResultListView = (ListView) findViewById(R.id.searchResultListView);
        m_searchResultListAdapter = new SearchResultListAdapter(this);
        m_searchResultListView.setAdapter(m_searchResultListAdapter);
        m_searchResultListView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if (position < 0 || position >= SearchActivity.this.m_results.length)
                    return;

                final SearchResult result = SearchActivity.this.m_results[position];
                final SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
                editor.putInt("currentBook", result.bookIndex);
                editor.putInt("currentChapter", result.chapterIndex);
                editor.putInt("currentVerse", result.verseIndex);
                editor.commit();

                final Bundle bundle = getIntent().getExtras();
                if (bundle != null && bundle.getBoolean("fromTextActivity", false))
                    SearchActivity.this.finish();
                else
                    SearchActivity.this.startActivity(new Intent(SearchActivity.this, TextActivity.class));

            }
        });
    }

    protected void onResume()
    {
        super.onResume();

        m_selectedTranslationShortName = getSharedPreferences("settings", MODE_PRIVATE).getString(
                "selectedTranslation", null);
        m_translationReader.selectTranslation(m_selectedTranslationShortName);

        final TranslationInfo[] translations = m_translationManager.translations();
        for (TranslationInfo translationInfo : translations) {
            if (translationInfo.installed && translationInfo.shortName.equals(m_selectedTranslationShortName)) {
                m_selectedTranslationTextView.setText(translationInfo.name);
                break;
            }
        }

        search(null);
    }

    public void search(View view)
    {
        final Editable searchToken = m_searchText.getText();
        if (searchToken.length() == 0)
            return;

        // TODO should the search functionality be moved to TranslationReader?
        new SearchAsyncTask().execute(searchToken);
    }

    private static class SearchResult
    {
        public int bookIndex;
        public int chapterIndex;
        public int verseIndex;
    }

    private class SearchAsyncTask extends AsyncTask<Editable, Void, Void>
    {
        protected void onPreExecute()
        {
            // running in the main thread

            SearchActivity.this.m_searchResultListAdapter.setTexts(null);

            InputMethodManager inputManager = (InputMethodManager) SearchActivity.this
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null) {
                inputManager.hideSoftInputFromWindow(SearchActivity.this.getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }

            m_progressDialog = new ProgressDialog(SearchActivity.this);
            m_progressDialog.setCancelable(false);
            m_progressDialog.setMessage(SearchActivity.this.getText(R.string.text_searching));
            m_progressDialog.show();
        }

        protected Void doInBackground(Editable... params)
        {
            // running in the worker thread

            final SQLiteDatabase db = SearchActivity.this.m_translationsDatabaseHelper.getReadableDatabase();
            final Cursor cursor = db.query(SearchActivity.this.m_selectedTranslationShortName, new String[] {
                    TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX,
                    TranslationsDatabaseHelper.COLUMN_VERSE_INDEX, TranslationsDatabaseHelper.COLUMN_TEXT },
                    TranslationsDatabaseHelper.COLUMN_TEXT + " LIKE ?", new String[] { "%" + params[0] + "%" }, null,
                    null, null);
            if (cursor != null) {
                final int count = cursor.getCount();
                if (count > 0) {
                    final int bookIndexColumnIndex = cursor
                            .getColumnIndex(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX);
                    final int chapterIndexColumnIndex = cursor
                            .getColumnIndex(TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX);
                    final int verseIndexColumnIndex = cursor
                            .getColumnIndex(TranslationsDatabaseHelper.COLUMN_VERSE_INDEX);
                    final int textColumnIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_TEXT);
                    m_texts = new String[count];
                    SearchActivity.this.m_results = new SearchResult[count];
                    int i = 0;
                    while (cursor.moveToNext()) {
                        final SearchResult result = new SearchResult();
                        result.bookIndex = cursor.getInt(bookIndexColumnIndex);
                        result.chapterIndex = cursor.getInt(chapterIndexColumnIndex);
                        result.verseIndex = cursor.getInt(verseIndexColumnIndex);
                        SearchActivity.this.m_results[i] = result;

                        String text = SearchActivity.this.m_translationReader.bookNames()[result.bookIndex];
                        text += " ";
                        text += Integer.toString(result.chapterIndex + 1);
                        text += ":";
                        text += Integer.toString(result.verseIndex + 1);
                        text += "\n";
                        text += cursor.getString(textColumnIndex);
                        m_texts[i++] = text;
                    }
                }
            }
            db.close();
            return null;
        }

        protected void onPostExecute(Void result)
        {
            // running in the main thread

            SearchActivity.this.m_searchResultListAdapter.setTexts(m_texts);
            m_progressDialog.dismiss();

            final CharSequence text = (m_texts == null ? "0" : Integer.toString(m_texts.length))
                    + SearchActivity.this.getResources().getText(R.string.text_search_result);
            Toast.makeText(SearchActivity.this, text, Toast.LENGTH_SHORT).show();
        }

        private ProgressDialog m_progressDialog;
        private String[] m_texts;
    }

    private class SearchResultListAdapter extends ListBaseAdapter
    {
        public SearchResultListAdapter(Context context)
        {
            super(context);
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
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
                textView.setTextColor(Color.BLACK);
                textView.setTypeface(null, Typeface.NORMAL);
            } else {
                textView = (TextView) convertView;
            }

            textView.setText(m_texts[position]);
            return textView;
        }
    }

    private EditText m_searchText;
    private ListView m_searchResultListView;
    private SearchResult[] m_results;
    private SearchResultListAdapter m_searchResultListAdapter;
    private String m_selectedTranslationShortName;
    private TextView m_selectedTranslationTextView;
    private TranslationsDatabaseHelper m_translationsDatabaseHelper;
    private TranslationManager m_translationManager;
    private TranslationReader m_translationReader;
}
