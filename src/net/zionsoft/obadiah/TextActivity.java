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

        final Bundle bundle = getIntent().getExtras();
        m_currentBook = bundle.getInt("selectedBook");
        m_currentChapter = bundle.getInt("selectedChapter");

        // initializes the tool bar buttons
        m_shareButton = (ImageButton) findViewById(R.id.shareButton);
        m_shareButton.setEnabled(false);

        m_copyButton = (ImageButton) findViewById(R.id.copyButton);
        m_copyButton.setEnabled(false);

        m_prevButton = (ImageButton) findViewById(R.id.prevButton);
        m_nextButton = (ImageButton) findViewById(R.id.nextButton);
        updateButtonState();

        // initializes verses list view
        m_listView = (ListView) findViewById(R.id.listView);

        m_listAdapter = new TextListAdapter(this);
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

        // initializes the title bar
        m_titleTranslationTextView = (TextView) findViewById(R.id.textTranslationSelection);
        m_titleTranslationTextView.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                startActivity(new Intent(TextActivity.this, TranslationSelectionActivity.class));
            }
        });
    }

    protected void onResume()
    {
        super.onResume();

        final SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        m_translationReader.selectTranslation(preferences.getString("selectedTranslation", null));

        setupUi();

        // scrolls to last read verse if necessary
        if (preferences.getInt("currentBook", -1) == m_currentBook
                && preferences.getInt("currentChapter", -1) == m_currentChapter)
            m_listView.setSelection(getSharedPreferences("settings", MODE_PRIVATE).getInt("currentVerse", 0));
    }

    protected void onPause()
    {
        super.onPause();

        final SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
        editor.putInt("currentBook", m_currentBook);
        editor.putInt("currentChapter", m_currentChapter);
        editor.putInt("currentVerse", m_listView.pointToPosition(0, 0));
        editor.commit();
    }

    public void changeChapter(View view)
    {
        if (view != m_prevButton && view != m_nextButton)
            return;

        if (view == m_prevButton) {
            if (m_currentChapter == 0)
                return;
            --m_currentChapter;
        } else {
            if (m_currentChapter == TranslationReader.chapterCount(m_currentBook) - 1)
                return;

            ++m_currentChapter;
        }

        updateButtonState();
        setupUi();
        m_listView.setSelectionAfterHeaderView();
    }

    public void share(View view)
    {
        if (m_listAdapter.hasItemSelected()) {
            final Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, m_listAdapter.selectedText());
            startActivity(Intent.createChooser(intent, getResources().getText(R.string.text_share_with)));
        }
    }

    public void copy(View view)
    {
        if (m_listAdapter.hasItemSelected()) {
            if (m_clipboardManager == null)
                m_clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            m_clipboardManager.setText(m_listAdapter.selectedText());
            Toast.makeText(this, R.string.text_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateButtonState()
    {
        if (m_currentChapter == 0)
            m_prevButton.setEnabled(false);
        else
            m_prevButton.setEnabled(true);

        if (m_currentChapter == TranslationReader.chapterCount(m_currentBook) - 1)
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
        m_listAdapter.setTexts(m_translationReader.verses(m_currentBook, m_currentChapter));
    }

    private static class TextListAdapter extends ListBaseAdapter
    {
        public TextListAdapter(TextActivity activity)
        {
            super(activity);
            m_textActivity = activity;
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

            final String prefix = m_textActivity.m_translationReader.bookNames()[m_textActivity.m_currentBook] + " "
                    + Integer.toString(m_textActivity.m_currentChapter + 1) + ":";
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
        private TextActivity m_textActivity;
    }

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
