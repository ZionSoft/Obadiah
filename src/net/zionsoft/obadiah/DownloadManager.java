package net.zionsoft.obadiah;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class DownloadManager extends Thread
{
    public static final int MESSAGE_DOWNLOAD_TRANSLATION = 1;

    public DownloadManager(Handler handler, TranslationDownloadActivity downloadActivity)
    {
        super();

        m_downloadActivityHandler = handler;
        m_downloadActivity = downloadActivity;
    }

    public void run()
    {
        getTranslationsList();

        Looper.prepare();
        m_handler = new Handler()
        {
            public void handleMessage(Message message)
            {
                switch (message.what) {
                case MESSAGE_DOWNLOAD_TRANSLATION: {
                    try {
                        TranslationInfo translationToDownload = m_availableTranslations[message.arg1];
                        String path = translationToDownload.path;

                        // creates sub-folder
                        File dir = new File(m_downloadActivity.getFilesDir(), path);
                        dir.mkdir();

                        URL url = new URL(BASE_URL + path + ".zip");
                        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

                        ZipInputStream zis = new ZipInputStream(
                                new BufferedInputStream(httpConnection.getInputStream()));
                        ZipEntry entry;
                        byte buffer[] = new byte[BUFFER_LENGTH];
                        int read = -1;
                        int downloaded = 0;
                        while ((entry = zis.getNextEntry()) != null) {
                            // unzips and writes to internal storage
                            FileOutputStream fos = new FileOutputStream(new File(dir, entry.getName()));
                            BufferedOutputStream os = new BufferedOutputStream(fos, BUFFER_LENGTH);
                            while ((read = zis.read(buffer, 0, BUFFER_LENGTH)) != -1)
                                os.write(buffer, 0, read);
                            os.flush();
                            os.close();

                            // notifies the progress
                            Message msg = Message.obtain(m_downloadActivityHandler,
                                    TranslationDownloadActivity.MESSAGE_DOWNLOAD_PROGRESS);
                            msg.arg1 = ++downloaded / 11;
                            m_downloadActivityHandler.sendMessage(msg);
                        }
                        zis.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // notifies the completion
                    m_downloadActivityHandler.sendMessage(Message.obtain(m_downloadActivityHandler,
                            TranslationDownloadActivity.MESSAGE_TRANSLATION_DOWNLOADED));
                    break;
                }
                }
            }
        };
        Looper.loop();
    }

    public Handler handler()
    {
        return m_handler;
    }

    public TranslationInfo[] availableTranslations()
    {
        return m_availableTranslations;
    }

    private void getTranslationsList()
    {
        try {
            URL url = new URL(BASE_URL + TRANSLATIONS_FILE);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            InputStream in = new BufferedInputStream(httpConnection.getInputStream());
            byte[] buffer = new byte[in.available()];
            in.read(buffer);
            in.close();

            TranslationInfo[] installedTranslations = BibleReader.getInstance().installedTranslations();
            int installedCount = installedTranslations.length;

            JSONArray replyArray = new JSONArray(new String(buffer, "UTF8"));
            final int length = replyArray.length();

            TranslationInfo[] availableTranslations = new TranslationInfo[length];
            int count = 0;
            for (int i = 0; i < length; ++i) {
                JSONObject translationObject = replyArray.getJSONObject(i);
                String path = translationObject.getString("path");
                int j = 0;
                for (; j < installedCount; ++j) {
                    if (installedTranslations[j].path.endsWith(path))
                        break;
                }
                if (j < installedCount)
                    continue;

                availableTranslations[count] = new TranslationInfo();
                availableTranslations[count].name = translationObject.getString("name");
                availableTranslations[count].path = path;
                availableTranslations[count].size = translationObject.getInt("size");
                ++count;
            }

            if (count > 0) {
                m_availableTranslations = new TranslationInfo[count];
                for (int i = 0; i < count; ++i)
                    m_availableTranslations[i] = availableTranslations[i];
            }

            m_downloadActivityHandler.sendMessage(Message.obtain(m_downloadActivityHandler,
                    TranslationDownloadActivity.MESSAGE_TRANSLATIONLIST_DOWNLOADED));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final int BUFFER_LENGTH = 2048;
    private static final String BASE_URL = "http://bible.zionsoft.net/";
    private static final String TRANSLATIONS_FILE = "translations.json";

    private Handler m_downloadActivityHandler;
    private Handler m_handler;
    private TranslationDownloadActivity m_downloadActivity;
    private TranslationInfo[] m_availableTranslations;
}
