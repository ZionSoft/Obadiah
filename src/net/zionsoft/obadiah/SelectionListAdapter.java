package net.zionsoft.obadiah;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SelectionListAdapter extends ListBaseAdapter
{
    public SelectionListAdapter(Context context, String[] texts)
    {
        super(context, texts);
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        TextView textView;
        if (convertView == null) {
            textView = new TextView(m_context);
            textView.setBackgroundDrawable(m_context.getResources().getDrawable(R.drawable.btn_button));
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
            textView.setPadding(10, 10, 10, 10);
            textView.setTextColor(Color.BLACK);
        } else {
            textView = (TextView) convertView;
        }

        textView.setText(m_texts[position]);
        return textView;

    }
}
