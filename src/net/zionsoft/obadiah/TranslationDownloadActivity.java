package net.zionsoft.obadiah;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class TranslationDownloadActivity extends Activity
{
    public static final int MESSAGE_TRANSLATIONLIST_DOWNLOADED = 1;
    public static final int MESSAGE_TRANSLATION_DOWNLOADED = 2;
    public static final int MESSAGE_DOWNLOAD_PROGRESS = 3;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_translations);
        setTitle(R.string.title_download_translation);

        ListView listView = (ListView) findViewById(R.id.listView);
        m_adapter = new SelectionListAdapter(this);
        listView.setAdapter(m_adapter);
        listView.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                m_progressDialog = new ProgressDialog(TranslationDownloadActivity.this);
                m_progressDialog.setCancelable(false);
                m_progressDialog.setMessage(getText(R.string.text_downloading));
                m_progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                m_progressDialog.setMax(100);
                m_progressDialog.setProgress(0);
                m_progressDialog.show();

                Handler handler = m_downloadManager.handler();
                Message message = Message.obtain(handler, DownloadManager.MESSAGE_DOWNLOAD_TRANSLATION);
                message.arg1 = position;
                handler.sendMessage(message);
            }
        });

        Handler handler = new Handler()
        {
            public void handleMessage(Message message)
            {
                switch (message.what) {
                case MESSAGE_TRANSLATIONLIST_DOWNLOADED: {
                    TranslationInfo[] availableTranslations = m_downloadManager.availableTranslations();
                    if (availableTranslations == null || availableTranslations.length == 0) {
                        m_progressDialog.dismiss();
                        Toast.makeText(TranslationDownloadActivity.this, R.string.text_no_available_translation,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        int length = availableTranslations.length;
                        String[] texts = new String[length];
                        for (int i = 0; i < length; ++i)
                            texts[i] = availableTranslations[i].name + " (" + availableTranslations[i].size + " KB)";
                        m_adapter.setTexts(texts);
                        m_progressDialog.dismiss();
                    }
                    break;
                }
                case MESSAGE_TRANSLATION_DOWNLOADED: {
                    BibleReader.getInstance().refresh();
                    m_progressDialog.dismiss();
                    break;
                }
                case MESSAGE_DOWNLOAD_PROGRESS: {
                    m_progressDialog.setProgress(message.arg1);
                    break;
                }
                }
            }
        };

        m_progressDialog = new ProgressDialog(this);
        m_progressDialog.setCancelable(false);
        m_progressDialog.setMessage(getText(R.string.text_downloading));
        m_progressDialog.show();

        m_downloadManager = new DownloadManager(handler, this);
        m_downloadManager.start();
    }

    private DownloadManager m_downloadManager;
    private ProgressDialog m_progressDialog;
    private SelectionListAdapter m_adapter;
}
