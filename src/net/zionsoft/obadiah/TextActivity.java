package net.zionsoft.obadiah;

import java.util.Iterator;
import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
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
        m_selectedTranslationTextView = (TextView) findViewById(R.id.selected_translation_textview);
        m_selectedTranslationTextView.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                startActivity(new Intent(TextActivity.this, TranslationSelectionActivity.class));
            }
        });
        m_selectedBookTextView = (TextView) findViewById(R.id.selected_book_textview);

        // initializes the tool bar buttons
        m_settingsButton = (ImageButton) findViewById(R.id.settings_button);
        m_searchButton = (ImageButton) findViewById(R.id.search_button);
        m_shareButton = (ImageButton) findViewById(R.id.share_button);
        m_copyButton = (ImageButton) findViewById(R.id.copy_button);

        // initializes verses view pager
        m_verseViewPager = (ViewPager) findViewById(R.id.verse_viewpager);
        m_versePagerAdapter = new VersePagerAdapter();
        m_verseViewPager.setAdapter(m_versePagerAdapter);
        m_verseViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            public void onPageScrollStateChanged(int state)
            {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {
            }

            public void onPageSelected(int position)
            {
                m_currentChapter = position;
                populateUi();
            }
        });
    }

    protected void onResume()
    {
        super.onResume();

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(SettingsActivity.PREF_NIGHTMODE, false)) {
            // night mode
            m_backgroundColor = Color.BLACK;
            m_textColor = Color.WHITE;
        } else {
            // day mode
            m_backgroundColor = Color.WHITE;
            m_textColor = Color.BLACK;
        }

        final SharedPreferences preferences = getSharedPreferences("settings", MODE_PRIVATE);
        m_currentBook = preferences.getInt("currentBook", 0);
        m_currentChapter = preferences.getInt("currentChapter", 0);
        m_translationReader.selectTranslation(preferences.getString("currentTranslation", null));

        m_verseViewPager.setCurrentItem(m_currentChapter);
        m_versePagerAdapter.setSelection(preferences.getInt("currentVerse", 0));
        m_versePagerAdapter.updateText();

        populateUi();
    }

    protected void onPause()
    {
        super.onPause();

        final SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
        editor.putInt("currentChapter", m_currentChapter);
        editor.putInt("currentVerse", m_versePagerAdapter.currentVerse());
        editor.commit();
    }

    public void onToolbarButtonClicked(View view)
    {
        if (view == m_settingsButton) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (view == m_shareButton) {
            if (m_versePagerAdapter.hasItemSelected()) {
                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, m_versePagerAdapter.selectedText());
                startActivity(Intent.createChooser(intent, getResources().getText(R.string.text_share_with)));
            }
        } else if (view == m_copyButton) {
            if (m_versePagerAdapter.hasItemSelected()) {
                if (m_clipboardManager == null)
                    m_clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                m_clipboardManager.setText(m_versePagerAdapter.selectedText());
                Toast.makeText(this, R.string.text_copied, Toast.LENGTH_SHORT).show();
            }
        } else if (view == m_searchButton) {
            startActivity(new Intent(this, SearchActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        }
    }

    private void populateUi()
    {
        m_selectedTranslationTextView.setText(m_translationReader.selectedTranslationShortName());
        m_selectedBookTextView.setText(m_translationReader.bookNames()[m_currentBook] + ", " + (m_currentChapter + 1));

        final boolean hasItemSelected = m_versePagerAdapter.hasItemSelected();
        m_shareButton.setEnabled(hasItemSelected);
        m_copyButton.setEnabled(hasItemSelected);
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
            m_selectedCount = 0;

            notifyDataSetChanged();
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            LinearLayout linearLayout;
            if (convertView == null) {
                linearLayout = new LinearLayout(m_context);
                for (int i = 0; i < 2; ++i) {
                    final TextView textView = new TextView(m_context);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                            m_context.getResources().getDimension(R.dimen.text_size));
                    textView.setPadding(10, 10, 10, 10);
                    linearLayout.addView(textView);
                }
            } else {
                linearLayout = (LinearLayout) convertView;
            }

            final TextView verseIndexTextView = (TextView) linearLayout.getChildAt(0);
            verseIndexTextView.setTextColor(TextActivity.this.m_textColor);
            verseIndexTextView.setText(Integer.toString(position + 1));

            final TextView verseTextView = (TextView) linearLayout.getChildAt(1);
            verseTextView.setTextColor(TextActivity.this.m_textColor);
            if (m_selected[position]) {
                final SpannableString string = new SpannableString(m_texts[position]);
                if (m_backgroundColorSpan == null)
                    m_backgroundColorSpan = new BackgroundColorSpan(Color.LTGRAY);
                string.setSpan(m_backgroundColorSpan, 0, m_texts[position].length(),
                        SpannableString.SPAN_INCLUSIVE_INCLUSIVE);
                verseTextView.setText(string);
            } else {
                verseTextView.setText(m_texts[position]);
            }

            return linearLayout;
        }

        private boolean m_selected[];
        private int m_selectedCount;
        private BackgroundColorSpan m_backgroundColorSpan;
    }

    private class VersePagerAdapter extends PagerAdapter
    {
        public VersePagerAdapter()
        {
            super();
            m_pages = new LinkedList<Page>();
        }

        public int getCount()
        {
            return (TextActivity.this.m_currentBook < 0) ? 0 : TranslationReader
                    .chapterCount(TextActivity.this.m_currentBook);
        }

        public Object instantiateItem(ViewGroup container, int position)
        {
            Iterator<Page> iterator = m_pages.iterator();
            Page page = null;
            while (iterator.hasNext()) {
                page = iterator.next();
                if (page.inUse) {
                    page = null;
                    continue;
                }
                break;
            }

            if (page == null) {
                page = new Page();
                m_pages.add(page);

                final ListView verseListView = new ListView(TextActivity.this);
                page.verseListView = verseListView;
                verseListView.setBackgroundColor(TextActivity.this.m_backgroundColor);
                verseListView.setCacheColorHint(TextActivity.this.m_backgroundColor);
                verseListView.setDivider(null);
                verseListView.setSelector(new ColorDrawable(Color.TRANSPARENT));

                final VerseListAdapter verseListAdapter = new VerseListAdapter(TextActivity.this);
                page.verseListAdapter = verseListAdapter;
                verseListView.setAdapter(verseListAdapter);
                verseListView.setOnItemClickListener(new OnItemClickListener()
                {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                    {
                        verseListAdapter.selectItem(position);
                        if (verseListAdapter.hasItemSelected()) {
                            TextActivity.this.m_shareButton.setEnabled(true);
                            TextActivity.this.m_copyButton.setEnabled(true);
                        } else {
                            TextActivity.this.m_shareButton.setEnabled(false);
                            TextActivity.this.m_copyButton.setEnabled(false);
                        }
                    }
                });
            }

            ((ViewPager) container).addView(page.verseListView, 0);
            page.inUse = true;
            page.position = position;
            page.verseListAdapter.setTexts(TextActivity.this.m_translationReader.verses(
                    TextActivity.this.m_currentBook, position));

            // scroll to the correct position
            if (m_selection > 0 && position == TextActivity.this.m_currentChapter) {
                page.verseListView.setSelection(m_selection);
                m_selection = 0;
            } else {
                page.verseListView.setSelectionAfterHeaderView();
            }

            return page;
        }

        public void destroyItem(ViewGroup container, int position, Object object)
        {
            Iterator<Page> iterator = m_pages.iterator();
            while (iterator.hasNext()) {
                final Page page = iterator.next();
                if (page.position == position) {
                    page.inUse = false;
                    ((ViewPager) container).removeView(page.verseListView);
                    return;
                }
            }
        }

        public boolean isViewFromObject(View view, Object object)
        {
            return view == ((Page) object).verseListView;
        }

        public void updateText()
        {
            Iterator<Page> iterator = m_pages.iterator();
            while (iterator.hasNext()) {
                final Page page = iterator.next();
                page.verseListView.setBackgroundColor(TextActivity.this.m_backgroundColor);
                page.verseListView.setCacheColorHint(TextActivity.this.m_backgroundColor);
                if (page.inUse) {
                    page.verseListAdapter.setTexts(TextActivity.this.m_translationReader.verses(
                            TextActivity.this.m_currentBook, page.position));
                }
            }

            notifyDataSetChanged();
        }

        public void setSelection(int selection)
        {
            m_selection = selection;
        }

        public int currentVerse()
        {
            Iterator<Page> iterator = m_pages.iterator();
            while (iterator.hasNext()) {
                final Page page = iterator.next();
                if (page.position == TextActivity.this.m_currentChapter)
                    return page.verseListView.pointToPosition(0, 0);
            }
            return 0;
        }

        public boolean hasItemSelected()
        {
            Iterator<Page> iterator = m_pages.iterator();
            while (iterator.hasNext()) {
                final Page page = iterator.next();
                if (page.position == TextActivity.this.m_currentChapter)
                    return page.verseListAdapter.hasItemSelected();
            }
            return false;
        }

        public String selectedText()
        {
            Iterator<Page> iterator = m_pages.iterator();
            while (iterator.hasNext()) {
                final Page page = iterator.next();
                if (page.position == TextActivity.this.m_currentChapter)
                    return page.verseListAdapter.selectedText();
            }
            return null;
        }

        private class Page
        {
            public boolean inUse;
            public int position;
            public ListView verseListView;
            public VerseListAdapter verseListAdapter;
        }

        private int m_selection;
        private LinkedList<Page> m_pages;
    }

    private int m_currentBook = -1;
    private int m_currentChapter = -1;
    private int m_backgroundColor;
    private int m_textColor;
    private ClipboardManager m_clipboardManager; // obsoleted by android.content.ClipboardManager since API level 11
    private ImageButton m_settingsButton;
    private ImageButton m_shareButton;
    private ImageButton m_copyButton;
    private ImageButton m_searchButton;
    private TextView m_selectedBookTextView;
    private TextView m_selectedTranslationTextView;
    private TranslationReader m_translationReader;
    private VersePagerAdapter m_versePagerAdapter;
    private ViewPager m_verseViewPager;
}
