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
        m_rootDir = rootDir;
        refresh();
    }

    public void refresh()
    {
        try {
            File[] directories = m_rootDir.listFiles();
            final int length = directories.length;
            if (length == 0)
                return;

            boolean hasTranslationsFile = false;
            for (int i = 0; i < length; ++i) {
                if (directories[i].isFile()) {
                    hasTranslationsFile = true;
                    break;
                }
            }

            if (hasTranslationsFile)
                m_installedTranslations = new TranslationInfo[length - 1];
            else
                m_installedTranslations = new TranslationInfo[length];
            for (int i = 0, index = 0; i < length; ++i) {
                if (directories[i].isFile())
                    continue;

                FileInputStream fis = new FileInputStream(new File(directories[i], BOOKS_FILE));
                byte[] buffer = new byte[fis.available()];
                fis.read(buffer);
                fis.close();

                m_installedTranslations[index] = new TranslationInfo();
                m_installedTranslations[index].path = directories[i].getAbsolutePath();

                JSONObject booksInfoObject = new JSONObject(new String(buffer, "UTF8"));
                m_installedTranslations[index].name = booksInfoObject.getString("name");

                JSONArray booksArray = booksInfoObject.getJSONArray("books");
                final int booksCount = booksArray.length();
                m_installedTranslations[index].bookName = new String[booksCount];
                for (int j = 0; j < booksCount; ++j)
                    m_installedTranslations[index].bookName[j] = booksArray.getString(j);

                ++index;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TranslationInfo[] installedTranslations()
    {
        return m_installedTranslations;
    }

    public void selectTranslation(String translation)
    {
        if (m_installedTranslations == null || m_installedTranslations.length == 0)
            return;

        if (translation == null) {
            m_selectedTranslation = m_installedTranslations[0].path;
            return;
        }

        int length = m_installedTranslations.length;
        for (int i = 0; i < length; ++i) {
            String path = m_installedTranslations[i].path;
            if (m_installedTranslations[i].path.endsWith(translation)) {
                m_selectedTranslation = path;
                return;
            }
        }
    }

    public TranslationInfo selectedTranslation()
    {
        if (m_selectedTranslation == null) {
            if (m_installedTranslations != null && m_installedTranslations.length > 0) {
                m_selectedTranslation = m_installedTranslations[0].path;
                return m_installedTranslations[0];
            } else {
                return null;
            }
        }

        int length = m_installedTranslations.length;
        for (int i = 0; i < length; ++i) {
            if (m_installedTranslations[i].path == m_selectedTranslation)
                return m_installedTranslations[i];
        }

        return null;
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
            String path = m_selectedTranslation + "/" + book + "-" + chapter + ".json";
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

    private File m_rootDir;
    private String m_selectedTranslation;
    private TranslationInfo[] m_installedTranslations;
}
