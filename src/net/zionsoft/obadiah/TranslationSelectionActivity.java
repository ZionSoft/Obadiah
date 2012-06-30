package net.zionsoft.obadiah;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class TranslationSelectionActivity extends Activity
{
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_translations);
        setTitle(R.string.title_select_translation);

        // set footer
        TextView textView = new TextView(this);
        textView.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_button));
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        textView.setPadding(10, 10, 10, 10);
        textView.setTextColor(Color.BLACK);
        textView.setText(R.string.button_download);
        textView.setOnClickListener(new OnClickListener()
        {
            public void onClick(View view)
            {
                // checks connectivity
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo == null || !networkInfo.isConnected()) {
                    Toast.makeText(TranslationSelectionActivity.this, R.string.text_no_network, Toast.LENGTH_LONG)
                            .show();
                    return;
                }
                // HTTP connection reuse was buggy before Froyo
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO)
                    System.setProperty("http.keepAlive", "false");

                Intent intent = new Intent(TranslationSelectionActivity.this, TranslationDownloadActivity.class);
                startActivity(intent);
            }
        });
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.addFooterView(textView);

        m_listAdapter = new SelectionListAdapter(this);
        listView.setAdapter(m_listAdapter);

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

    protected void onResume()
    {
        super.onResume();

        TranslationInfo[] installedTranslations = BibleReader.getInstance().installedTranslations();
        int translationCount = installedTranslations.length;
        if (translationCount == 0)
            return;
        String[] translationNames = new String[translationCount];
        for (int i = 0; i < translationCount; ++i)
            translationNames[i] = installedTranslations[i].name;
        m_listAdapter.setTexts(translationNames);
    }

    private SelectionListAdapter m_listAdapter;
}
