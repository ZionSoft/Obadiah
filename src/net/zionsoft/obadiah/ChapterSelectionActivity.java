package net.zionsoft.obadiah;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

public class ChapterSelectionActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chapterselection_activity);

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

        // updates the title
        // TODO no need to update if selected translation is not changed
        TranslationInfo translationInfo = BibleReader.getInstance().selectedTranslation();
        if (translationInfo == null)
            return;
        m_titleTranslationTextView.setText(translationInfo.shortName);
        
        if (m_titleBookNameTextView == null)
            m_titleBookNameTextView = (TextView) findViewById(R.id.textBookName);
        m_titleBookNameTextView.setText(translationInfo.bookName[m_selectedBook]);
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
    private TextView m_titleBookNameTextView;
    private TextView m_titleTranslationTextView;
}
