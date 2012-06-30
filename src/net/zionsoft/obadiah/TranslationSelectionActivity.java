package net.zionsoft.obadiah;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class TranslationSelectionActivity extends Activity
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_translationselection);
        setTitle(R.string.title_select_translation);

        TranslationInfo[] translationInfo = BibleReader.getInstance().availableTranslations();
        int translationCount = translationInfo.length;
        String[] translationNames = new String[translationCount];
        for (int i = 0; i < translationCount; ++i)
            translationNames[i] = translationInfo[i].name;

        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(new SelectionListAdapter(this, translationNames));
        listView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
                editor.putInt("selectedTranslation", position);
                editor.commit();

                BibleReader.getInstance().selectTranslation(position);

                finish();
            }
        });
    }
}
