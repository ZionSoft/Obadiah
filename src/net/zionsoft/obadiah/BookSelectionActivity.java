package net.zionsoft.obadiah;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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

        BibleReader bibleReader = BibleReader.getInstance();
        // NOTE must call this before BibleReader is further used
        bibleReader.setRootDir(getFilesDir());

        // loads the translation as used last time
        SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
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
        GridView gridView = (GridView) findViewById(R.id.gridView);
        gridView.setColumnWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 144, getResources()
                .getDisplayMetrics()));
        m_listAdapter = new SelectionListAdapter(this);
        gridView.setAdapter(m_listAdapter);
        gridView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                startChapterSelectionActivity(false, position);
            }
        });

        // initializes the UI based on the selected translation
        setupUi();
    }

    protected void onResume()
    {
        super.onResume();

        // opens dialog if no translation installed
        // the code is here in case the user doesn't download anything
        final String[] list = getFilesDir().list();
        final int length = list.length;
        if (length == 0 || (length == 1 && list[0].equals(TRANSLATIONS_FILE))) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(R.string.text_no_translation).setCancelable(false)
                    .setPositiveButton(R.string.text_yes, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            dialog.dismiss();
                            startTranslationSelectionActivity();
                        }
                    }).setNegativeButton(R.string.text_no, new DialogInterface.OnClickListener()
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

        // updates the UI if resumed from TranslationSelectionActivity
        if (m_fromTranslationSelectionActivity)
            setupUi();
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_book_selection_activity, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu)
    {
        if (m_continueReadingMenuItem == null)
            m_continueReadingMenuItem = menu.findItem(R.id.menu_continue_reading);

        if (m_canContinueReading) {
            m_continueReadingMenuItem.setVisible(true);
        } else {
            // only needs to check if resumed from ChapterSelectionActivity,
            // which opens TextActivity, which sets "currentBook"
            if (m_fromChapterSelectionActivity
                    && getSharedPreferences("settings", MODE_PRIVATE).getInt("currentBook", -1) >= 0) {
                m_continueReadingMenuItem.setVisible(true);
                m_canContinueReading = true;
            } else {
                m_continueReadingMenuItem.setVisible(false);
            }
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_continue_reading: {
            startChapterSelectionActivity(true, getSharedPreferences("settings", MODE_PRIVATE).getInt("currentBook", 0));
            return true;
        }
        case R.id.menu_select_translation: {
            startTranslationSelectionActivity();
            return true;
        }
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void startChapterSelectionActivity(boolean continueReading, int selectedBook)
    {
        m_fromChapterSelectionActivity = true;
        m_fromTranslationSelectionActivity = false;

        Intent intent = new Intent(this, ChapterSelectionActivity.class);
        intent.putExtra("continueReading", continueReading);
        intent.putExtra("selectedBook", selectedBook);
        startActivity(intent);
    }

    private void startTranslationSelectionActivity()
    {
        m_fromChapterSelectionActivity = false;
        m_fromTranslationSelectionActivity = true;

        Intent intent = new Intent(this, TranslationSelectionActivity.class);
        startActivity(intent);
    }

    private void setupUi()
    {
        TranslationInfo translationInfo = BibleReader.getInstance().selectedTranslation();
        if (translationInfo == null)
            return;
        setTitle(translationInfo.name);
        m_listAdapter.setTexts(translationInfo.bookName);
    }

    private static final String TRANSLATIONS_FILE = "translations.json";

    private boolean m_fromChapterSelectionActivity;
    private boolean m_fromTranslationSelectionActivity;
    private boolean m_canContinueReading;
    private MenuItem m_continueReadingMenuItem;
    private SelectionListAdapter m_listAdapter;
}
