package net.zionsoft.obadiah;

import android.app.Activity;
import android.content.Intent;
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
        bibleReader.setAssetManager(getAssets());
        bibleReader.selectTranslation(getSharedPreferences("settings", MODE_PRIVATE).getInt("selectedTranslation", 0));

        GridView gridView = (GridView) findViewById(R.id.gridView);
        gridView.setColumnWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 144, getResources()
                .getDisplayMetrics()));
        m_listAdapter = new SelectionListAdapter(this, bibleReader.selectedTranslation().bookName);
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

    private SelectionListAdapter m_listAdapter;
}
