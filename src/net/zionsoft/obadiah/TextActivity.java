package net.zionsoft.obadiah;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class TextActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.text_activity);

        final Bundle bundle = getIntent().getExtras();
        m_currentBook = bundle.getInt("selectedBook");
        m_currentChapter = bundle.getInt("selectedChapter");

        m_translationReader = new TranslationReader(this);
        m_translationReader.selectTranslation(bundle.getString("selectedTranslationShortName"));

        m_listAdapter = new TextListAdapter(this);
        m_titleTranslationTextView = (TextView) findViewById(R.id.textTranslationSelection);
        setupUi();

        m_shareButton = (ImageButton) findViewById(R.id.shareButton);
        m_shareButton.setEnabled(false);

        m_copyButton = (ImageButton) findViewById(R.id.copyButton);
        m_copyButton.setEnabled(false);

        m_listView = (ListView) findViewById(R.id.listView);
        m_listView.setAdapter(m_listAdapter);
        m_listView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                m_listAdapter.selectItem(position);
                if (m_listAdapter.hasItemSelected()) {
                    m_shareButton.setEnabled(true);
                    m_copyButton.setEnabled(true);
                } else {
                    m_shareButton.setEnabled(false);
                    m_copyButton.setEnabled(false);
                }
            }
        });

        m_prevButton = (ImageButton) findViewById(R.id.prevButton);
        m_nextButton = (ImageButton) findViewById(R.id.nextButton);
        updateButtonState();

        // initializes the title bar
        m_titleTranslationTextView.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                startTranslationSelectionActivity();
            }
        });

        // scrolls to last read verse if necessary
        m_listView.setSelection(bundle.getInt("selectedVerse", 0));
    }

    protected void onResume()
    {
        super.onResume();

        if (m_fromTranslationSelection) {
            m_fromTranslationSelection = false;
            setupUi();
        }
    }

    protected void onPause()
    {
        super.onPause();

        SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
        editor.putString("selectedTranslation", m_translationReader.selectedTranslationShortName());
        editor.putInt("currentBook", m_currentBook);
        editor.putInt("currentChapter", m_currentChapter);
        editor.putInt("currentVerse", m_listView.pointToPosition(0, 0));
        editor.commit();
    }

    public void previousChapter(View view)
    {
        if (m_currentChapter == 0)
            return;

        --m_currentChapter;
        updateButtonState();
        setupUi();
        m_listView.setSelectionAfterHeaderView();
    }

    public void nextChapter(View view)
    {
        if (m_currentChapter == m_translationReader.chapterCount(m_currentBook) - 1)
            return;

        ++m_currentChapter;
        updateButtonState();
        setupUi();
        m_listView.setSelectionAfterHeaderView();
    }

    public void share(View view)
    {
        if (m_listAdapter.hasItemSelected()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, selectedText());
            startActivity(Intent.createChooser(intent, getResources().getText(R.string.text_share_with)));
        }
    }

    public void copy(View view)
    {
        if (m_listAdapter.hasItemSelected()) {
            if (m_clipboardManager == null)
                m_clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            m_clipboardManager.setText(selectedText());
            Toast.makeText(this, R.string.text_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private String selectedText()
    {
        if (!m_listAdapter.hasItemSelected())
            return null;
        String[] texts = m_listAdapter.selectedTexts();
        String selected = null;
        for (String text : texts) {
            if (selected == null)
                selected = m_translationReader.bookNames()[m_currentBook] + " " + (m_currentChapter + 1) + ":" + text;
            else
                selected += ("\n" + m_translationReader.bookNames()[m_currentBook] + " " + (m_currentChapter + 1) + ":" + text);
        }
        return selected;
    }

    private void updateButtonState()
    {
        if (m_currentChapter == 0)
            m_prevButton.setEnabled(false);
        else
            m_prevButton.setEnabled(true);

        if (m_currentChapter == m_translationReader.chapterCount(m_currentBook) - 1)
            m_nextButton.setEnabled(false);
        else
            m_nextButton.setEnabled(true);
    }

    private void setupUi()
    {
        m_titleTranslationTextView.setText(m_translationReader.selectedTranslationShortName());

        if (m_titleBookNameTextView == null)
            m_titleBookNameTextView = (TextView) findViewById(R.id.textBookName);
        m_titleBookNameTextView.setText(m_translationReader.bookNames()[m_currentBook] + ", " + (m_currentChapter + 1));

        // TODO handles if the translation is corrupted
        String[] verses = m_translationReader.verses(m_currentBook, m_currentChapter);
        m_listAdapter.setTexts(verses);
    }

    private void startTranslationSelectionActivity()
    {
        m_fromTranslationSelection = true;
        Intent intent = new Intent(this, TranslationSelectionActivity.class);
        startActivity(intent);
    }

    private boolean m_fromTranslationSelection;
    private int m_currentBook;
    private int m_currentChapter;
    private ClipboardManager m_clipboardManager; // Obsoleted by android.content.ClipboardManager since API level 11.
    private ImageButton m_prevButton;
    private ImageButton m_nextButton;
    private ImageButton m_shareButton;
    private ImageButton m_copyButton;
    private ListView m_listView;
    private TextListAdapter m_listAdapter;
    private TextView m_titleBookNameTextView;
    private TextView m_titleTranslationTextView;
    private TranslationReader m_translationReader;
}
