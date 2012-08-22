package net.zionsoft.obadiah;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
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
        m_listAdapter = new TextListAdapter(this);
        setupUi();

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

        // initialize the menu button handler
        Button button = (Button) findViewById(R.id.menuButton);
        button.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(TextActivity.this);
                dialogBuilder.setTitle(R.string.dialog_menu_title);

                CharSequence[] items = null;
                Resources resources = TextActivity.this.getResources();
                if (m_listAdapter.hasItemSelected()) {
                    items = new CharSequence[2];
                    items[0] = resources.getText(R.string.menu_select_translation);
                    items[1] = resources.getText(R.string.menu_share);
                } else {
                    items = new CharSequence[1];
                    items[0] = resources.getText(R.string.menu_select_translation);
                }
                dialogBuilder.setItems(items, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();

                        switch (which) {
                        case 0: // select translation
                            startTranslationSelectionActivity();
                            break;
                        case 1: // share
                            startShareActivity();
                            break;
                        }
                    }
                });

                dialogBuilder.create().show();
            }
        });

        // scrolls to last read verse if opened as continue reading
        if (bundle.getBoolean("continueReading", false))
            m_listView.setSelection(getSharedPreferences("settings", MODE_PRIVATE).getInt("currentVerse", 0));
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
        if (m_currentChapter == m_bibleReader.chapterCount(m_currentBook) - 1)
            return;

        ++m_currentChapter;
        updateButtonState();
        setupUi();
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

    private void setupUi()
    {
        m_translationInfo = m_bibleReader.selectedTranslation();
        if (m_translationInfo == null) {
            Toast.makeText(this, R.string.text_no_selected_translation, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (m_titleTextView == null)
            m_titleTextView = (TextView) findViewById(R.id.titleText);
        m_titleTextView.setText(m_translationInfo.bookName[m_currentBook] + ", " + (m_currentChapter + 1));

        // TODO handles if the translation is corrupted
        String[] verses = m_bibleReader.verses(m_currentBook, m_currentChapter);
        m_listAdapter.setTexts(verses);
    }

    private void startShareActivity()
    {
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
                    content += ("\n" + m_translationInfo.bookName[m_currentBook] + " " + (m_currentChapter + 1) + ":" + text);
                }
            }
            intent.putExtra(Intent.EXTRA_TEXT, content);

            startActivity(Intent.createChooser(intent, getResources().getText(R.string.text_share_with)));
        }
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
    private BibleReader m_bibleReader;
    private Button m_prevButton;
    private Button m_nextButton;
    private ListView m_listView;
    private TextListAdapter m_listAdapter;
    private TextView m_titleTextView;
    private TranslationInfo m_translationInfo;
}
