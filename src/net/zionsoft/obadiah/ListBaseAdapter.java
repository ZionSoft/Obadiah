package net.zionsoft.obadiah;

import android.content.Context;
import android.widget.BaseAdapter;

public abstract class ListBaseAdapter extends BaseAdapter
{
    public ListBaseAdapter(Context context, String[] texts)
    {
        super();

        m_context = context;
        m_texts = texts;
    }

    public void setTexts(String[] texts)
    {
        if (m_texts != texts) {
            m_texts = texts;
            notifyDataSetChanged();
        }
    }

    public int getCount()
    {
        return (m_texts == null) ? 0 : m_texts.length;
    }

    public Object getItem(int position)
    {
        return position;
    }

    public long getItemId(int position)
    {
        return position;
    }

    protected Context m_context;
    protected String[] m_texts;
}
