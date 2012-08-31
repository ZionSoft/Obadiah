package net.zionsoft.obadiah;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TextListAdapter extends ListBaseAdapter
{
    public TextListAdapter(Context context)
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

    public String[] selectedTexts()
    {
        if (!hasItemSelected())
            return null;

        String[] texts = new String[m_selectedCount];
        int length = m_texts.length;
        int index = 0;
        for (int i = 0; i < length; ++i) {
            if (m_selected[i])
                texts[index++] = Integer.toString(i + 1) + " " + m_texts[i];
        }
        return texts;
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
                TextView textView = new TextView(m_context);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
                textView.setPadding(10, 10, 10, 10);
                textView.setTextColor(Color.BLACK);
                linearLayout.addView(textView);
            }
        } else {
            linearLayout = (LinearLayout) convertView;
        }

        TextView verseIndex = (TextView) linearLayout.getChildAt(0);
        verseIndex.setText(Integer.toString(position + 1));

        TextView verse = (TextView) linearLayout.getChildAt(1);
        if (m_selected[position]) {
            SpannableString string = new SpannableString(m_texts[position]);
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
