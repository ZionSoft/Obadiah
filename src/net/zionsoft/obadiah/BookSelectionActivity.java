package net.zionsoft.obadiah;

import java.io.File;

import android.app.Activity;
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
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class BookSelectionActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_gridview);

        BibleReader bibleReader = BibleReader.getInstance();
        bibleReader.setRootDir(getFilesDir());

        SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);

        String selectedTranslation = null;
        try {
            selectedTranslation = preferences.getString("selectedTranslation", null);
        } catch (ClassCastException e) {
            // the value is an integer for 1.0 and 1.1
            int selected = preferences.getInt("selectedTranslation", 0);
            if (selected == 0)
                selectedTranslation = "authorized-king-james";
            else if (selected == 1)
                selectedTranslation = "chinese-union-simplified";
        }

        bibleReader.selectTranslation(selectedTranslation);

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

    protected void onStart()
    {
        super.onStart();

        File filesDir = getFilesDir();
        if (filesDir.list().length == 0) {
            // no translation installed
            Toast.makeText(this, R.string.text_no_translation, Toast.LENGTH_LONG).show();
            openTranslationSelectionActivity();
        }
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
            openTranslationSelectionActivity();
            return true;
        }
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void openTranslationSelectionActivity()
    {
        Intent intent = new Intent(this, TranslationSelectionActivity.class);
        startActivity(intent);
    }

    private SelectionListAdapter m_listAdapter;
}
