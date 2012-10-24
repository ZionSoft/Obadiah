package net.zionsoft.obadiah;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

        m_translationReader = new TranslationReader(this);

        // initializes the title bar
        m_selectedTranslationTextView = (TextView) findViewById(R.id.textTranslationSelection);
        m_selectedTranslationTextView.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                startActivity(new Intent(TextActivity.this, TranslationSelectionActivity.class));
            }
        });
        m_selectedBookTextView = (TextView) findViewById(R.id.textBookName);

        // initializes the tool bar buttons
        m_searchButton = (ImageButton) findViewById(R.id.searchButton);

        m_shareButton = (ImageButton) findViewById(R.id.shareButton);
        m_shareButton.setEnabled(false);

        m_copyButton = (ImageButton) findViewById(R.id.copyButton);
        m_copyButton.setEnabled(false);

        m_previousChapterButton = (ImageButton) findViewById(R.id.previousChapterButton);
        m_nextChapterButton = (ImageButton) findViewById(R.id.nextChapterButton);
        updateChangeChapterButtonState();

        // initializes verses list view
        m_verseListView = (ListView) findViewById(R.id.verseListView);
        m_verseListAdapter = new VerseListAdapter(this);
        m_verseListView.setAdapter(m_verseListAdapter);
        m_verseListView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                TextActivity.this.m_verseListAdapter.selectItem(position);
                if (TextActivity.this.m_verseListAdapter.hasItemSelected()) {
                    TextActivity.this.m_shareButton.setEnabled(true);
                    TextActivity.this.m_copyButton.setEnabled(true);
                } else {
                    TextActivity.this.m_shareButton.setEnabled(false);
                    TextActivity.this.m_copyButton.setEnabled(false);
                }
            }
        });
    }

    protected void onResume()
    {
        super.onResume();

        final SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        m_currentBook = preferences.getInt("currentBook", 0);
        m_currentChapter = preferences.getInt("currentChapter", 0);
        m_translationReader.selectTranslation(preferences.getString("currentTranslation", null));

        populateUi();

        // scrolls to last read verse
        m_verseListView.setSelection(preferences.getInt("currentVerse", 0));
    }

    protected void onPause()
    {
        super.onPause();

        final SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
        editor.putInt("currentChapter", m_currentChapter);
        editor.putInt("currentVerse", m_verseListView.pointToPosition(0, 0));
        editor.commit();
    }

    public void onToolbarButtonClicked(View view)
    {
        if (view == m_shareButton) {
            if (m_verseListAdapter.hasItemSelected()) {
                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, m_verseListAdapter.selectedText());
                startActivity(Intent.createChooser(intent, getResources().getText(R.string.text_share_with)));
            }
        } else if (view == m_copyButton) {
            if (m_verseListAdapter.hasItemSelected()) {
                if (m_clipboardManager == null)
                    m_clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                m_clipboardManager.setText(m_verseListAdapter.selectedText());
                Toast.makeText(this, R.string.text_copied, Toast.LENGTH_SHORT).show();
            }
        } else if (view == m_searchButton) {
            final Intent intent = new Intent(this, SearchActivity.class);
            intent.putExtra("fromTextActivity", true);
            startActivity(intent);
        } else if (view == m_previousChapterButton || view == m_nextChapterButton) {
            if (view == m_previousChapterButton) {
                if (m_currentChapter == 0)
                    return;
                --m_currentChapter;
            } else {
                if (m_currentChapter == TranslationReader.chapterCount(m_currentBook) - 1)
                    return;

                ++m_currentChapter;
            }

            updateChangeChapterButtonState();
            populateUi();
            m_verseListView.setSelectionAfterHeaderView();
        }
    }

    private void updateChangeChapterButtonState()
    {
        if (m_currentChapter == 0)
            m_previousChapterButton.setEnabled(false);
        else
            m_previousChapterButton.setEnabled(true);

        if (m_currentChapter == TranslationReader.chapterCount(m_currentBook) - 1)
            m_nextChapterButton.setEnabled(false);
        else
            m_nextChapterButton.setEnabled(true);
    }

    private void populateUi()
    {
        m_selectedTranslationTextView.setText(m_translationReader.selectedTranslationShortName());
        m_selectedBookTextView.setText(m_translationReader.bookNames()[m_currentBook] + ", " + (m_currentChapter + 1));
        m_verseListAdapter.setTexts(m_translationReader.verses(m_currentBook, m_currentChapter));
    }

    private class VerseListAdapter extends ListBaseAdapter
    {
        public VerseListAdapter(Context context)
        {
            super(context);
        }

        public void selectItem(int position)
        {
            if (position < 0 || position >= m_texts.length)
                return;

            m_selected[position] ^= true;
            if (m_selected[position])
                ++m_selectedCount;
            else
                --m_selectedCount;

            notifyDataSetChanged();
        }

        public boolean hasItemSelected()
        {
            return (m_selectedCount > 0);
        }

        public String selectedText()
        {
            if (!hasItemSelected())
                return null;

            // format: <book name> <chapter index>:<verse index> <verse text>
            final String prefix = TextActivity.this.m_translationReader.bookNames()[TextActivity.this.m_currentBook]
                    + " " + Integer.toString(TextActivity.this.m_currentChapter + 1) + ":";
            String selected = null;
            for (int i = 0; i < m_texts.length; ++i) {
                if (m_selected[i]) {
                    if (selected != null) {
                        selected += "\n";
                        selected += prefix;
                    } else {
                        selected = prefix;
                    }
                    selected += Integer.toString(i + 1);
                    selected += " ";
                    selected += m_texts[i];
                }
            }
            return selected;
        }

        public void setTexts(String[] texts)
        {
            m_texts = texts;

            final int length = texts.length;
            if (m_selected == null || length > m_selected.length)
                m_selected = new boolean[length];
            for (int i = 0; i < length; ++i)
                m_selected[i] = false;

            notifyDataSetChanged();
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            LinearLayout linearLayout;
            if (convertView == null) {
                linearLayout = new LinearLayout(m_context);
                for (int i = 0; i < 2; ++i) {
                    final TextView textView = new TextView(m_context);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
                    textView.setPadding(10, 10, 10, 10);
                    textView.setTextColor(Color.BLACK);
                    linearLayout.addView(textView);
                }
            } else {
                linearLayout = (LinearLayout) convertView;
            }

            final TextView verseIndex = (TextView) linearLayout.getChildAt(0);
            verseIndex.setText(Integer.toString(position + 1));

            final TextView verse = (TextView) linearLayout.getChildAt(1);
            if (m_selected[position]) {
                final SpannableString string = new SpannableString(m_texts[position]);
                if (m_backgroundColorSpan == null)
                    m_backgroundColorSpan = new BackgroundColorSpan(Color.LTGRAY);
                string.setSpan(m_backgroundColorSpan, 0, m_texts[position].length(),
                        SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
                verse.setText(string);
            } else {
                verse.setText(m_texts[position]);
            }

            return linearLayout;
        }

        private boolean m_selected[];
        private int m_selectedCount;
        private BackgroundColorSpan m_backgroundColorSpan;
    }

    private int m_currentBook;
    private int m_currentChapter;
    private ClipboardManager m_clipboardManager; // obsoleted by android.content.ClipboardManager since API level 11
    private ImageButton m_previousChapterButton;
    private ImageButton m_nextChapterButton;
    private ImageButton m_shareButton;
    private ImageButton m_copyButton;
    private ImageButton m_searchButton;
    private ListView m_verseListView;
    private TextView m_selectedBookTextView;
    private TextView m_selectedTranslationTextView;
    private TranslationReader m_translationReader;
    private VerseListAdapter m_verseListAdapter;
}
