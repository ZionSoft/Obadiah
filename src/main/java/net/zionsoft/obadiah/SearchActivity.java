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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;

import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.bible.TranslationManager;
import net.zionsoft.obadiah.bible.TranslationReader;
import net.zionsoft.obadiah.bible.TranslationsDatabaseHelper;
import net.zionsoft.obadiah.util.SettingsManager;

public class SearchActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        m_settingsManager = new SettingsManager(this);
        m_translationsDatabaseHelper = new TranslationsDatabaseHelper(this);
        m_translationManager = new TranslationManager(this);
        m_translationReader = new TranslationReader(this);

        // initializes the search bar
        m_searchText = (EditText) findViewById(R.id.search_edittext);
        m_searchText.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    SearchActivity.this.search(null);
                    return true;
                }
                return false;
            }
        });

        // initializes the search results list view
        m_searchResultListView = (ListView) findViewById(R.id.search_result_listview);
        m_searchResultListAdapter = new SearchResultListAdapter(this);
        m_searchResultListView.setAdapter(m_searchResultListAdapter);
        m_searchResultListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= SearchActivity.this.m_results.length)
                    return;

                final SearchResult result = SearchActivity.this.m_results[position];
                final SharedPreferences.Editor editor = getSharedPreferences(Constants.SETTING_KEY, MODE_PRIVATE)
                        .edit();
                editor.putInt(Constants.CURRENT_BOOK_SETTING_KEY, result.bookIndex);
                editor.putInt(Constants.CURRENT_CHAPTER_SETTING_KEY, result.chapterIndex);
                editor.putInt(Constants.CURRENT_VERSE_SETTING_KEY, result.verseIndex);
                editor.commit();

                SearchActivity.this.startActivity(new Intent(SearchActivity.this, TextActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));

            }
        });
    }

    protected void onResume() {
        super.onResume();

        m_settingsManager.refresh();
        final int backgroundColor = m_settingsManager.backgroundColor();
        m_searchResultListView.setBackgroundColor(backgroundColor);
        m_searchResultListView.setCacheColorHint(backgroundColor);
        m_textColor = m_settingsManager.textColor();
        m_textSize = m_settingsManager.textSize();

        m_searchResultListAdapter.notifyDataSetChanged();

        final String selectedTranslationShortName = getSharedPreferences(Constants.SETTING_KEY, MODE_PRIVATE)
                .getString(Constants.CURRENT_TRANSLATION_SETTING_KEY, null);
        if (selectedTranslationShortName == null
                || !selectedTranslationShortName.equals(m_selectedTranslationShortName)) {
            m_translationReader.selectTranslation(selectedTranslationShortName);
            m_selectedTranslationShortName = m_translationReader.selectedTranslationShortName();

            final TranslationInfo[] translations = m_translationManager.translations();
            for (TranslationInfo translationInfo : translations) {
                if (translationInfo.installed && translationInfo.shortName.equals(m_selectedTranslationShortName)) {
                    setTitle(translationInfo.name);
                    break;
                }
            }

            search(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select_translation:
                startActivity(new Intent(SearchActivity.this, TranslationSelectionActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void search(View view) {
        final Editable searchToken = m_searchText.getText();
        if (searchToken.length() == 0)
            return;

        // TODO should the search functionality be moved to TranslationReader?
        new SearchAsyncTask().execute(searchToken);
    }

    private static class SearchResult {
        public int bookIndex;
        public int chapterIndex;
        public int verseIndex;
    }

    private class SearchAsyncTask extends AsyncTask<Editable, Void, Void> {
        protected void onPreExecute() {
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

        protected Void doInBackground(Editable... params) {
            // running in the worker thread

            final SQLiteDatabase db = SearchActivity.this.m_translationsDatabaseHelper.getReadableDatabase();
            final Cursor cursor = db.query(SearchActivity.this.m_selectedTranslationShortName, new String[]{
                    TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX,
                    TranslationsDatabaseHelper.COLUMN_VERSE_INDEX, TranslationsDatabaseHelper.COLUMN_TEXT},
                    TranslationsDatabaseHelper.COLUMN_TEXT + " LIKE ?", new String[]{"%"
                    + params[0].toString().trim().replaceAll("\\s+", "%") + "%"}, null, null, null);
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

        protected void onPostExecute(Void result) {
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

    private class SearchResultListAdapter extends ListBaseAdapter {
        public SearchResultListAdapter(Context context) {
            super(context);
        }

        public void setTexts(String[] texts) {
            m_texts = texts;
            notifyDataSetChanged();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                textView = new TextView(m_context);
                textView.setTypeface(null, Typeface.NORMAL);
            } else {
                textView = (TextView) convertView;
            }

            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, SearchActivity.this.m_textSize);
            textView.setTextColor(SearchActivity.this.m_textColor);
            textView.setText(m_texts[position]);
            return textView;
        }
    }

    private int m_textColor;
    private float m_textSize;
    private EditText m_searchText;
    private ListView m_searchResultListView;
    private SearchResult[] m_results;
    private SearchResultListAdapter m_searchResultListAdapter;
    private SettingsManager m_settingsManager;
    private String m_selectedTranslationShortName;
    private TranslationsDatabaseHelper m_translationsDatabaseHelper;
    private TranslationManager m_translationManager;
    private TranslationReader m_translationReader;
}
