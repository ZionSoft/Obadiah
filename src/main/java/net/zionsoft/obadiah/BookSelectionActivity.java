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

import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.bible.TranslationManager;
import net.zionsoft.obadiah.bible.TranslationReader;
import net.zionsoft.obadiah.support.UpgradeAsyncTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class BookSelectionActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bookselection_activity);

        // convert to new format from old format if needed
        final int currentApplicationVersion = getSharedPreferences(Constants.SETTING_KEY, MODE_PRIVATE).getInt(
                Constants.CURRENT_APPLICATION_VERSION_SETTING_KEY, 0);
        if (currentApplicationVersion < Constants.CURRENT_APPLICATION_VERSION) {
            m_upgrading = true;
            new UpgradeAsyncTask(this).execute();
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        m_padding = (int) getResources().getDimension(R.dimen.padding);

        m_settingsManager = new SettingsManager(this);
        m_translationManager = new TranslationManager(this);
        m_translationReader = new TranslationReader(this);

        // initializes the tool bar
        m_settingsButton = (ImageButton) findViewById(R.id.settings_button);
        m_searchButton = (ImageButton) findViewById(R.id.search_button);

        // initializes the title bar
        m_selectedTranslationTextView = (TextView) findViewById(R.id.selected_translation_textview);
        m_selectedTranslationTextView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                BookSelectionActivity.this.startTranslationSelectionActivity();
            }
        });
        m_selectedBookTextView = (TextView) findViewById(R.id.selected_book_textview);

        // initializes the book names list view
        m_bookListView = (ListView) findViewById(R.id.book_listview);
        m_bookListAdapter = new BookListAdapter(this);
        m_bookListView.setAdapter(m_bookListAdapter);
        m_bookListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (BookSelectionActivity.this.m_selectedBook == position)
                    return;

                BookSelectionActivity.this.m_selectedBook = position;
                BookSelectionActivity.this.m_selectedBookTextView.setText(m_translationReader.bookNames()[m_selectedBook]);
                BookSelectionActivity.this.m_bookListAdapter.notifyDataSetChanged();
                BookSelectionActivity.this.updateChapterSelectionListAdapter();
                BookSelectionActivity.this.m_chaptersGridView.setSelection(0);
            }
        });

        // initializes the chapters selection grid view
        m_chaptersGridView = (GridView) findViewById(R.id.chapter_gridview);
        m_chapterListAdapter = new ChapterListAdapter(this);
        m_chaptersGridView.setAdapter(m_chapterListAdapter);
        m_chaptersGridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final SharedPreferences preferences = BookSelectionActivity.this.getSharedPreferences(
                        Constants.SETTING_KEY, MODE_PRIVATE);
                if (preferences.getInt(Constants.CURRENT_BOOK_SETTING_KEY, -1) != m_selectedBook
                        || preferences.getInt(Constants.CURRENT_CHAPTER_SETTING_KEY, -1) != position) {
                    final SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt(Constants.CURRENT_BOOK_SETTING_KEY, m_selectedBook);
                    editor.putInt(Constants.CURRENT_CHAPTER_SETTING_KEY, position);
                    editor.putInt(Constants.CURRENT_VERSE_SETTING_KEY, 0);
                    editor.commit();
                }

                startActivity(new Intent(BookSelectionActivity.this, TextActivity.class));
            }
        });
    }

    protected void onResume() {
        super.onResume();

        m_settingsManager.refresh();
        final int backgroundColor = m_settingsManager.backgroundColor();
        m_bookListView.setBackgroundColor(backgroundColor);
        m_bookListView.setCacheColorHint(backgroundColor);
        m_chaptersGridView.setBackgroundColor(backgroundColor);
        m_chaptersGridView.setCacheColorHint(backgroundColor);
        m_textColor = m_settingsManager.textColor();
        m_textSize = m_settingsManager.textSize();

        if (m_upgrading)
            return;

        populateUi();
    }

    public void onUpgradeFinished() {
        populateUi();
        m_upgrading = false;
    }

    public void onToolbarButtonClicked(View view) {
        if (view == m_settingsButton)
            startActivity(new Intent(this, SettingsActivity.class));
        else if (view == m_searchButton)
            startActivity(new Intent(this, SearchActivity.class));
    }

    private void populateUi() {
        boolean hasInstalledTranslation = false;
        final TranslationInfo[] translations = m_translationManager.translations();
        if (translations != null) {
            for (TranslationInfo translationInfo : translations) {
                if (translationInfo.installed) {
                    hasInstalledTranslation = true;
                    break;
                }
            }
        }

        if (hasInstalledTranslation) {
            // loads last used translation
            final SharedPreferences preferences = getSharedPreferences(Constants.SETTING_KEY, MODE_PRIVATE);
            m_translationReader.selectTranslation(preferences
                    .getString(Constants.CURRENT_TRANSLATION_SETTING_KEY, null));

            // loads the last read book and chapter
            m_currentBook = preferences.getInt(Constants.CURRENT_BOOK_SETTING_KEY, -1);
            m_currentChapter = preferences.getInt(Constants.CURRENT_CHAPTER_SETTING_KEY, -1);

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
        } else {
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(R.string.text_no_translation).setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            BookSelectionActivity.this.startTranslationSelectionActivity();
                        }
                    }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    BookSelectionActivity.this.finish();
                }
            });
            alertDialogBuilder.create().show();
        }
    }

    private void updateChapterSelectionListAdapter() {
        final int chapterCount = TranslationReader.chapterCount(m_selectedBook);
        final String[] chapters = new String[chapterCount];
        for (int i = 0; i < chapterCount; ++i)
            chapters[i] = Integer.toString(i + 1);
        m_chapterListAdapter.setTexts(chapters);
    }

    private void startTranslationSelectionActivity() {
        startActivity(new Intent(this, TranslationSelectionActivity.class));
    }

    private class BookListAdapter extends ListBaseAdapter {
        public BookListAdapter(Context context) {
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
                textView.setGravity(Gravity.CENTER);
                textView.setPadding(BookSelectionActivity.this.m_padding, BookSelectionActivity.this.m_padding,
                        BookSelectionActivity.this.m_padding, BookSelectionActivity.this.m_padding);
                // textView.setHeight(m_textViewHeight);
                textView.setBackgroundResource(R.drawable.list_item_background);
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
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, BookSelectionActivity.this.m_textSize);
            textView.setTextColor(BookSelectionActivity.this.m_textColor);
            textView.setText(m_texts[position]);
            return textView;
        }
    }

    public class ChapterListAdapter extends ListBaseAdapter {
        public ChapterListAdapter(Context context) {
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
                textView.setBackgroundResource(R.drawable.list_item_background);
                textView.setGravity(Gravity.CENTER);
                textView.setPadding(BookSelectionActivity.this.m_padding, BookSelectionActivity.this.m_padding,
                        BookSelectionActivity.this.m_padding, BookSelectionActivity.this.m_padding);
            } else {
                textView = (TextView) convertView;
            }

            if (m_currentBook == m_selectedBook && m_currentChapter == position)
                textView.setTypeface(null, Typeface.BOLD);
            else
                textView.setTypeface(null, Typeface.NORMAL);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, BookSelectionActivity.this.m_textSize);
            textView.setTextColor(BookSelectionActivity.this.m_textColor);
            textView.setText(m_texts[position]);
            return textView;
        }
    }

    private boolean m_upgrading = false;
    private int m_currentBook = -1;
    private int m_currentChapter = -1;
    private int m_selectedBook = -1;
    private int m_padding;
    private int m_textColor;
    private float m_textSize;
    private BookListAdapter m_bookListAdapter;
    private ChapterListAdapter m_chapterListAdapter;
    private ImageButton m_settingsButton;
    private ImageButton m_searchButton;
    private GridView m_chaptersGridView;
    private ListView m_bookListView;
    private SettingsManager m_settingsManager;
    private TextView m_selectedBookTextView;
    private TextView m_selectedTranslationTextView;
    private TranslationManager m_translationManager;
    private TranslationReader m_translationReader;
}
