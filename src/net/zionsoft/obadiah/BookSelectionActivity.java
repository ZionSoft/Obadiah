package net.zionsoft.obadiah;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
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

public class BookSelectionActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bookselection_activity);

        // convert to new format from old format if needed
        if (getSharedPreferences("settings", MODE_PRIVATE).getInt("currentApplicationVersion", 0) < CURRENT_APPLICATION_VERSION) {
            m_converting = true;
            new UpgradeAsyncTask().execute();
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        m_translationManager = new TranslationManager(this);
        m_translationReader = new TranslationReader(this);

        // initializes the tool bar
        m_settingsButton = (ImageButton) findViewById(R.id.settingsButton);
        m_searchButton = (ImageButton) findViewById(R.id.searchButton);

        // initializes the title bar
        m_selectedTranslationTextView = (TextView) findViewById(R.id.selectedTranslationTextView);
        m_selectedTranslationTextView.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                BookSelectionActivity.this.startTranslationSelectionActivity();
            }
        });
        m_selectedBookTextView = (TextView) findViewById(R.id.selectedBookNameTextView);

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
        m_chaptersGridView = (GridView) findViewById(R.id.chapterGridView);
        m_chapterListAdapter = new ChapterListAdapter(this);
        m_chaptersGridView.setAdapter(m_chapterListAdapter);
        m_chaptersGridView.setOnItemClickListener(new OnItemClickListener()
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

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(SettingsActivity.PREF_NIGHTMODE, false)) {
            // night mode
            m_bookListView.setBackgroundColor(Color.BLACK);
            m_bookListView.setCacheColorHint(Color.BLACK);
            m_chaptersGridView.setBackgroundColor(Color.BLACK);
            m_chaptersGridView.setCacheColorHint(Color.BLACK);
            m_textColor = Color.WHITE;
        } else {
            // day mode
            m_bookListView.setBackgroundColor(Color.WHITE);
            m_bookListView.setCacheColorHint(Color.WHITE);
            m_chaptersGridView.setBackgroundColor(Color.WHITE);
            m_chaptersGridView.setCacheColorHint(Color.WHITE);
            m_textColor = Color.BLACK;
        }

        if (m_converting)
            return;

        populateUi();
    }

    public void onToolbarButtonClicked(View view)
    {
        if (view == m_settingsButton)
            startActivity(new Intent(this, SettingsActivity.class));
        else if (view == m_searchButton)
            startActivity(new Intent(this, SearchActivity.class));
    }

    private void populateUi()
    {
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
            final SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
            m_translationReader.selectTranslation(preferences.getString("currentTranslation", null));

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
        } else {
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
            textView.setTextColor(BookSelectionActivity.this.m_textColor);
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
            } else {
                textView = (TextView) convertView;
            }

            if (m_currentBook == m_selectedBook && m_currentChapter == position)
                textView.setTypeface(null, Typeface.BOLD);
            else
                textView.setTypeface(null, Typeface.NORMAL);
            textView.setTextColor(BookSelectionActivity.this.m_textColor);
            textView.setText(m_texts[position]);
            return textView;
        }

        private int m_textViewHeight;
    }

    private class UpgradeAsyncTask extends AsyncTask<Void, Integer, Void>
    {
        protected void onPreExecute()
        {
            // running in the main thread

            m_progressDialog = new ProgressDialog(BookSelectionActivity.this);
            m_progressDialog.setCancelable(false);
            m_progressDialog.setMessage(BookSelectionActivity.this.getText(R.string.text_initializing));
            m_progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            m_progressDialog.setMax(100);
            m_progressDialog.setProgress(0);
            m_progressDialog.show();
        }

        protected Void doInBackground(Void... params)
        {
            // running in the worker thread

            convertTranslations();
            publishProgress(98);

            convertSettings();
            publishProgress(99);

            // sets the application version
            final SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("currentApplicationVersion", CURRENT_APPLICATION_VERSION);
            editor.commit();

            publishProgress(100);

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

            BookSelectionActivity.this.populateUi();
            m_progressDialog.dismiss();
            BookSelectionActivity.this.m_converting = false;
        }

        private void convertSettings()
        {
            // old settings format is used prior to 1.5.0

            final SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
            final SharedPreferences.Editor editor = preferences.edit();
            String selectedTranslation = null;
            try {
                selectedTranslation = preferences.getString("selectedTranslation", null);
                if (selectedTranslation != null) {
                    if (selectedTranslation.equals("danske-bibel1871"))
                        selectedTranslation = "DA1871";
                    else if (selectedTranslation.equals("authorized-king-james"))
                        selectedTranslation = "KJV";
                    else if (selectedTranslation.equals("american-king-james"))
                        selectedTranslation = "AKJV";
                    else if (selectedTranslation.equals("basic-english"))
                        selectedTranslation = "BBE";
                    else if (selectedTranslation.equals("esv"))
                        selectedTranslation = "ESV";
                    else if (selectedTranslation.equals("raamattu1938"))
                        selectedTranslation = "PR1938";
                    else if (selectedTranslation.equals("fre-segond"))
                        selectedTranslation = "FreSegond";
                    else if (selectedTranslation.equals("darby-elb1905"))
                        selectedTranslation = "Elb1905";
                    else if (selectedTranslation.equals("luther-biblia"))
                        selectedTranslation = "Lut1545";
                    else if (selectedTranslation.equals("italian-diodati-bibbia"))
                        selectedTranslation = "Dio";
                    else if (selectedTranslation.equals("korean-revised"))
                        selectedTranslation = "개역성경";
                    else if (selectedTranslation.equals("biblia-almeida-recebida"))
                        selectedTranslation = "PorAR";
                    else if (selectedTranslation.equals("reina-valera1569"))
                        selectedTranslation = "RV1569";
                    else if (selectedTranslation.equals("chinese-union-traditional"))
                        selectedTranslation = "華語和合本";
                    else if (selectedTranslation.equals("chinese-union-simplified"))
                        selectedTranslation = "中文和合本";
                    else if (selectedTranslation.equals("chinese-new-version-traditional"))
                        selectedTranslation = "華語新譯本";
                    else if (selectedTranslation.equals("chinese-new-version-simplified"))
                        selectedTranslation = "中文新译本";
                }
            } catch (ClassCastException e) {
                // the value is an integer prior to 1.2.0
                final int selected = preferences.getInt("selectedTranslation", 0);
                if (selected == 1)
                    selectedTranslation = "中文和合本";
                else
                    selectedTranslation = "KJV";
            }
            final TranslationInfo[] translations = BookSelectionActivity.this.m_translationManager.translations();
            if (translations != null) {
                for (TranslationInfo translation : translations) {
                    if (translation.shortName.equals(selectedTranslation))
                        editor.putString("currentTranslation", selectedTranslation);
                }
            }
            editor.remove("selectedTranslation");

            editor.commit();
        }

        private void convertTranslations()
        {
            // old translations format is used prior to 1.5.0

            final File rootDir = BookSelectionActivity.this.getFilesDir();
            final BibleReader oldReader = new BibleReader(rootDir);
            final TranslationInfo[] installedTranslations = oldReader.installedTranslations();
            if (installedTranslations == null || installedTranslations.length == 0)
                return;

            final SQLiteDatabase db = new TranslationsDatabaseHelper(BookSelectionActivity.this).getWritableDatabase();
            db.beginTransaction();
            try {
                final ContentValues versesValues = new ContentValues(4);
                final ContentValues bookNamesValues = new ContentValues(3);
                final ContentValues translationInfoValues = new ContentValues(5);
                translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_INSTALLED, 1);

                final double progressDelta = 1.36 / installedTranslations.length;
                double currentProgress = 1.0;
                for (TranslationInfo translationInfo : installedTranslations) {
                    publishProgress((int) currentProgress);

                    // creates a translation table
                    db.execSQL("CREATE TABLE " + translationInfo.shortName + " ("
                            + TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + " INTEGER NOT NULL, "
                            + TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX + " INTEGER NOT NULL, "
                            + TranslationsDatabaseHelper.COLUMN_VERSE_INDEX + " INTEGER NOT NULL, "
                            + TranslationsDatabaseHelper.COLUMN_TEXT + " TEXT NOT NULL);");
                    db.execSQL("CREATE INDEX INDEX_" + translationInfo.shortName + " ON " + translationInfo.shortName
                            + " (" + TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + ", "
                            + TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX + ", "
                            + TranslationsDatabaseHelper.COLUMN_VERSE_INDEX + ");");

                    bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME,
                            translationInfo.shortName);

                    oldReader.selectTranslation(translationInfo.path);
                    for (int bookIndex = 0; bookIndex < 66; ++bookIndex) {
                        // writes verses
                        final int chapterCount = TranslationReader.chapterCount(bookIndex);
                        for (int chapterIndex = 0; chapterIndex < chapterCount; ++chapterIndex) {
                            String[] texts = oldReader.verses(bookIndex, chapterIndex);
                            int verseIndex = 0;
                            for (String text : texts) {
                                versesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, bookIndex);
                                versesValues.put(TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX, chapterIndex);
                                versesValues.put(TranslationsDatabaseHelper.COLUMN_VERSE_INDEX, verseIndex++);
                                versesValues.put(TranslationsDatabaseHelper.COLUMN_TEXT, text);
                                db.insert(translationInfo.shortName, null, versesValues);
                            }
                        }

                        // writes book name
                        bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, bookIndex);
                        bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_NAME,
                                translationInfo.bookNames[bookIndex]);
                        db.insert(TranslationsDatabaseHelper.TABLE_BOOK_NAMES, null, bookNamesValues);

                        currentProgress += progressDelta;
                        publishProgress((int) currentProgress);
                    }

                    // adds to the translations table
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_NAME, translationInfo.name);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME,
                            translationInfo.shortName);

                    if (translationInfo.shortName.equals("DA1871")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1843);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Dansk");
                    } else if (translationInfo.shortName.equals("KJV")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1817);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "English");
                    } else if (translationInfo.shortName.equals("AKJV")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1799);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "English");
                    } else if (translationInfo.shortName.equals("BBE")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1826);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "English");
                    } else if (translationInfo.shortName.equals("ESV")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1780);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "English");
                    } else if (translationInfo.shortName.equals("PR1938")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1950);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Suomi");
                    } else if (translationInfo.shortName.equals("FreSegond")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1972);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Français");
                    } else if (translationInfo.shortName.equals("Elb1905")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1990);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Deutsche");
                    } else if (translationInfo.shortName.equals("Lut1545")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1880);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Deutsche");
                    } else if (translationInfo.shortName.equals("Dio")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, "Italiano");
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, 1843);
                    } else if (translationInfo.shortName.equals("개역성경")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1923);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "한국인");
                    } else if (translationInfo.shortName.equals("PorAR")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1950);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Português");
                    } else if (translationInfo.shortName.equals("RV1569")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1855);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Español");
                    } else if (translationInfo.shortName.equals("華語和合本")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1772);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "正體中文");
                    } else if (translationInfo.shortName.equals("中文和合本")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1739);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "简体中文");
                    } else if (translationInfo.shortName.equals("華語新譯本")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1874);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "正體中文");
                    } else if (translationInfo.shortName.equals("中文新译本")) {
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1877);
                        translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "简体中文");
                    }

                    db.insert(TranslationsDatabaseHelper.TABLE_TRANSLATIONS, null, translationInfoValues);
                }

                publishProgress(91);
                db.setTransactionSuccessful();
                Utils.removeDirectory(rootDir);
            } finally {
                publishProgress(92);
                db.endTransaction();
                db.close();
            }
        }

        private ProgressDialog m_progressDialog;
    }

    private static int CURRENT_APPLICATION_VERSION = 10500;

    private boolean m_converting = false;
    private int m_currentBook = -1;
    private int m_currentChapter = -1;
    private int m_selectedBook = -1;
    private int m_textColor;
    private BookListAdapter m_bookListAdapter;
    private ChapterListAdapter m_chapterListAdapter;
    private ImageButton m_settingsButton;
    private ImageButton m_searchButton;
    private GridView m_chaptersGridView;
    private ListView m_bookListView;
    private TextView m_selectedBookTextView;
    private TextView m_selectedTranslationTextView;
    private TranslationManager m_translationManager;
    private TranslationReader m_translationReader;
}
