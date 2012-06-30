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

        int chapterCount = BibleReader.getInstance().chapterCount(m_selectedBook);
        String[] chapters = new String[chapterCount];
        for (int i = 0; i < chapterCount; ++i)
            chapters[i] = Integer.toString(i + 1);

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
                Intent intent = new Intent(ChapterSelectionActivity.this, TextActivity.class);
                intent.putExtra("selectedBook", m_selectedBook);
                intent.putExtra("selectedChapter", position);
                startActivity(intent);
            }
        });

        if (bundle.getBoolean("continueReading", false)) {
            Intent intent = new Intent(this, TextActivity.class);
            intent.putExtra("continueReading", true);
            intent.putExtra("selectedBook", m_selectedBook);
            intent.putExtra("selectedChapter",
                    getSharedPreferences("settings", MODE_PRIVATE).getInt("currentChapter", 0));
            startActivity(intent);
        }
    }

    protected void onResume()
    {
        super.onResume();

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
            Intent intent = new Intent(this, TranslationSelectionActivity.class);
            startActivity(intent);
            return true;
        }
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private int m_selectedBook;
}
