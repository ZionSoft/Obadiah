package net.zionsoft.obadiah;

import java.io.File;
import java.io.FileInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

public class BibleReader
{
    public static BibleReader getInstance()
    {
        if (instance == null)
            instance = new BibleReader();
        return instance;
    }

    public void setRootDir(File rootDir)
    {
        try {
            File[] directories = rootDir.listFiles();
            final int length = directories.length;
            m_translationInfo = new TranslationInfo[length];
            for (int i = 0; i < length; ++i) {
                FileInputStream fis = new FileInputStream(new File(directories[i], BOOKS_FILE));
                byte[] buffer = new byte[fis.available()];
                fis.read(buffer);
                fis.close();

                m_translationInfo[i] = new TranslationInfo();
                m_translationInfo[i].path = directories[i].getAbsolutePath();

                JSONObject booksInfoObject = new JSONObject(new String(buffer, "UTF8"));
                m_translationInfo[i].name = booksInfoObject.getString("name");

                JSONArray booksArray = booksInfoObject.getJSONArray("books");
                final int booksCount = booksArray.length();
                m_translationInfo[i].bookName = new String[booksCount];
                for (int j = 0; j < booksCount; ++j)
                    m_translationInfo[i].bookName[j] = booksArray.getString(j);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TranslationInfo[] availableTranslations()
    {
        return m_translationInfo;
    }

    public void selectTranslation(int index)
    {
        if (index < 0 || m_translationInfo == null || index >= m_translationInfo.length)
            return;

        m_selectedTranslation = index;
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
            String path = m_translationInfo[m_selectedTranslation].path + "/" + book + "-" + chapter + ".json";
            FileInputStream fis = new FileInputStream(new File(path));
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();

            JSONObject jsonObject = new JSONObject(new String(buffer, "UTF8"));
            JSONArray paragraphArray = jsonObject.getJSONArray("verses");
            int paragraphCount = paragraphArray.length();
            String[] paragraphs = new String[paragraphCount];
            for (int i = 0; i < paragraphCount; ++i)
                paragraphs[i] = paragraphArray.getString(i);
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
    private static final String BOOKS_FILE = "books.json";

    private static BibleReader instance;

    private int m_selectedTranslation;
    private TranslationInfo[] m_translationInfo;
}
