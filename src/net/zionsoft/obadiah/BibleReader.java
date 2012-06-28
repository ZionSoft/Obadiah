package net.zionsoft.obadiah;

import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.res.AssetManager;

public class BibleReader
{
    public static BibleReader getInstance()
    {
        if (instance == null)
            instance = new BibleReader();
        return instance;
    }

    public void setAssetManager(AssetManager assetManager)
    {
        m_assetManager = assetManager;
    }

    public TranslationInfo[] availableTranslations()
    {
        try {
            if (m_translationInfo == null) {
                InputStream inputStream = m_assetManager.open("bible/translations.json", AssetManager.ACCESS_BUFFER);
                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);
                JSONArray booksArray = new JSONArray(new String(buffer, "UTF8"));
                int bookCount = booksArray.length();
                m_translationInfo = new TranslationInfo[bookCount];
                for (int i = 0; i < bookCount; ++i) {
                    JSONObject bookObject = booksArray.getJSONObject(i);
                    m_translationInfo[i] = new TranslationInfo();
                    m_translationInfo[i].name = bookObject.getString("name");
                    m_translationInfo[i].path = bookObject.getString("path");
                }
                inputStream.close();
            }
            return m_translationInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void selectTranslation(int index)
    {
        availableTranslations();
        if (index < 0 || m_translationInfo == null || index >= m_translationInfo.length)
            return;

        m_selectedTranslation = index;
        TranslationInfo translationInfo = m_translationInfo[m_selectedTranslation];
        if (translationInfo.bookName != null)
            return;

        try {
            InputStream inputStream = m_assetManager.open("bible/" + translationInfo.path + "/books.json",
                    AssetManager.ACCESS_BUFFER);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            JSONObject translationInfoObject = new JSONObject(new String(buffer, "UTF8"));
            JSONArray booksArray = translationInfoObject.getJSONArray("books");
            int bookCount = booksArray.length();
            translationInfo.bookName = new String[bookCount];
            for (int i = 0; i < bookCount; ++i)
                translationInfo.bookName[i] = booksArray.getString(i);
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TranslationInfo selectedTranslation()
    {
        return (m_selectedTranslation == -1) ? null : m_translationInfo[m_selectedTranslation];
    }

    public int chapterCount(int book)
    {
        if (book < 0 || book >= CHAPTER_COUNT.length)
            return 0;
        return CHAPTER_COUNT[book];
    }

    public String[] verses(int book, int chapter)
    {
        try {
            String path = "bible/" + m_translationInfo[m_selectedTranslation].path + "/" + book + "-" + chapter
                    + ".json";
            InputStream inputStream = m_assetManager.open(path, AssetManager.ACCESS_BUFFER);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            JSONObject jsonObject = new JSONObject(new String(buffer, "UTF8"));
            JSONArray paragraphArray = jsonObject.getJSONArray("verses");
            int paragraphCount = paragraphArray.length();
            String[] paragraphs = new String[paragraphCount];
            for (int i = 0; i < paragraphCount; ++i)
                paragraphs[i] = paragraphArray.getString(i);
            inputStream.close();
            return paragraphs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private BibleReader()
    {
        // do nothing
    }

    static {
        CHAPTER_COUNT = new int[] { 50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150, 31, 12,
                8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4, 28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4,
                5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22 };
    }

    private static final int[] CHAPTER_COUNT;
    private static BibleReader instance;
    private AssetManager m_assetManager;
    private TranslationInfo[] m_translationInfo;
    private int m_selectedTranslation = -1;
}
