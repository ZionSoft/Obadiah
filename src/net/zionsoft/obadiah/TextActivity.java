package net.zionsoft.obadiah;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

public class TextActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_text_activity);

        Bundle bundle = getIntent().getExtras();
        m_currentBook = bundle.getInt("selectedBook");
        m_currentChapter = bundle.getInt("selectedChapter");

        m_bibleReader = BibleReader.getInstance();
        m_translationInfo = m_bibleReader.selectedTranslation();

        m_listAdapter = new TextListAdapter(this, m_bibleReader.verses(m_currentBook, m_currentChapter));
        m_listView = (ListView) findViewById(R.id.listView);
        m_listView.setAdapter(m_listAdapter);

        m_prevButton = (Button) findViewById(R.id.prevButton);
        m_nextButton = (Button) findViewById(R.id.nextButton);
        updateButtonState();
        
        if (bundle.getBoolean("continueReading", false))
            m_listView.setSelection(getSharedPreferences("settings", MODE_PRIVATE).getInt("currentVerse", 0));
    }
    
    protected void onResume()
    {
        super.onResume();

        m_translationInfo = m_bibleReader.selectedTranslation();
        setTitle(m_translationInfo.bookName[m_currentBook] + ", " + (m_currentChapter + 1));
        m_listAdapter.setTexts(m_bibleReader.verses(m_currentBook, m_currentChapter));
    }

    protected void onPause()
    {
        super.onPause();

        SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
        editor.putInt("currentBook", m_currentBook);
        editor.putInt("currentChapter", m_currentChapter);
        editor.putInt("currentVerse", m_listView.pointToPosition(0, 0));
        editor.commit();
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

    public void previousChapter(View view)
    {
        if (m_currentChapter == 0)
            return;

        --m_currentChapter;
        updateButtonState();
        setTitle(m_translationInfo.bookName[m_currentBook] + ", " + (m_currentChapter + 1));
        m_listAdapter.setTexts(m_bibleReader.verses(m_currentBook, m_currentChapter));
        m_listView.setSelectionAfterHeaderView();
    }

    public void nextChapter(View view)
    {
        if (m_currentChapter == m_bibleReader.chapterCount(m_currentBook) - 1)
            return;

        ++m_currentChapter;
        updateButtonState();
        setTitle(m_translationInfo.bookName[m_currentBook] + ", " + (m_currentChapter + 1));
        m_listAdapter.setTexts(m_bibleReader.verses(m_currentBook, m_currentChapter));
        m_listView.setSelectionAfterHeaderView();
    }

    private void updateButtonState()
    {
        if (m_currentChapter == 0)
            m_prevButton.setEnabled(false);
        else
            m_prevButton.setEnabled(true);

        if (m_currentChapter == m_bibleReader.chapterCount(m_currentBook) - 1)
            m_nextButton.setEnabled(false);
        else
            m_nextButton.setEnabled(true);
    }

    private int m_currentBook;
    private int m_currentChapter;
    private BibleReader m_bibleReader;
    private TranslationInfo m_translationInfo;
    private Button m_prevButton;
    private Button m_nextButton;
    private TextListAdapter m_listAdapter;
    ListView m_listView;
}
