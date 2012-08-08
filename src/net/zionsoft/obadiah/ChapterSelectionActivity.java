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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class ChapterSelectionActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_gridview);

        Bundle bundle = getIntent().getExtras();
        m_selectedBook = bundle.getInt("selectedBook");

        // creates strings for chapter listing
        final int chapterCount = BibleReader.getInstance().chapterCount(m_selectedBook);
        String[] chapters = new String[chapterCount];
        for (int i = 0; i < chapterCount; ++i)
            chapters[i] = Integer.toString(i + 1);

        // initializes UI for chapter selection
        GridView gridView = (GridView) findViewById(R.id.gridView);
        gridView.setColumnWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources()
                .getDisplayMetrics()));

        SelectionListAdapter adapter = new SelectionListAdapter(this);
        adapter.setTexts(chapters);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                startTextActivity(false, m_selectedBook, position);
            }
        });

        // starts TextActivity if this activity is opened as continue reading
        if (bundle.getBoolean("continueReading", false)) {
            startTextActivity(true, m_selectedBook,
                    getSharedPreferences("settings", MODE_PRIVATE).getInt("currentChapter", 0));
        }
    }

    protected void onResume()
    {
        super.onResume();

        // updates the title
        // TODO no need to update if selected translation is not changed
        setTitle(BibleReader.getInstance().selectedTranslation().bookName[m_selectedBook]);
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_select_translation: {
            startTranslationSelectionActivity();
            return true;
        }
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void startTextActivity(boolean continueReading, int selectedBook, int selectedChapter)
    {
        Intent intent = new Intent(ChapterSelectionActivity.this, TextActivity.class);
        intent.putExtra("continueReading", continueReading);
        intent.putExtra("selectedBook", selectedBook);
        intent.putExtra("selectedChapter", selectedChapter);
        startActivity(intent);
    }

    private void startTranslationSelectionActivity()
    {
        Intent intent = new Intent(this, TranslationSelectionActivity.class);
        startActivity(intent);
    }

    private int m_selectedBook;
}
