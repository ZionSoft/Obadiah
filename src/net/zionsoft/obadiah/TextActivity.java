package net.zionsoft.obadiah;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

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
        setTitle(m_translationInfo.bookName[m_currentBook] + ", " + (m_currentChapter + 1));

        m_listAdapter = new TextListAdapter(this);
        m_listAdapter.setTexts(m_bibleReader.verses(m_currentBook, m_currentChapter));

        m_listView = (ListView) findViewById(R.id.listView);
        m_listView.setAdapter(m_listAdapter);
        m_listView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                m_listAdapter.selectItem(position);
            }
        });

        m_prevButton = (Button) findViewById(R.id.prevButton);
        m_nextButton = (Button) findViewById(R.id.nextButton);
        updateButtonState();

        if (bundle.getBoolean("continueReading", false))
            m_listView.setSelection(getSharedPreferences("settings", MODE_PRIVATE).getInt("currentVerse", 0));
    }

    protected void onResume()
    {
        super.onResume();

        if (m_fromTranslationSelection) {
            m_fromTranslationSelection = false;
            m_translationInfo = m_bibleReader.selectedTranslation();
            setTitle(m_translationInfo.bookName[m_currentBook] + ", " + (m_currentChapter + 1));
            m_listAdapter.setTexts(m_bibleReader.verses(m_currentBook, m_currentChapter));
        }
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
        inflater.inflate(R.menu.menu_text_activity, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu)
    {
        if (m_shareMenuItem == null)
            m_shareMenuItem = menu.findItem(R.id.menu_share);
        if (m_listAdapter.hasItemSelected())
            m_shareMenuItem.setVisible(true);
        else
            m_shareMenuItem.setVisible(false);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_share: {
            if (m_listAdapter.hasItemSelected()) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("text/plain");

                String[] selectedTexts = m_listAdapter.selectedTexts();
                String content = null;
                for (String text : selectedTexts) {
                    if (content == null) {
                        content = m_translationInfo.bookName[m_currentBook] + " " + (m_currentChapter + 1) + ":" + text;
                    } else {
                        content += ("\n" + m_translationInfo.bookName[m_currentBook] + " " + (m_currentChapter + 1)
                                + ":" + text);
                    }
                }
                intent.putExtra(Intent.EXTRA_TEXT, content);

                startActivity(Intent.createChooser(intent, getResources().getText(R.string.text_share_with)));
            }
            return true;
        }
        case R.id.menu_select_translation: {
            m_fromTranslationSelection = true;
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

    private boolean m_fromTranslationSelection;
    private int m_currentBook;
    private int m_currentChapter;
    private BibleReader m_bibleReader;
    private TranslationInfo m_translationInfo;
    private Button m_prevButton;
    private Button m_nextButton;
    private MenuItem m_shareMenuItem;
    private TextListAdapter m_listAdapter;
    ListView m_listView;
}
