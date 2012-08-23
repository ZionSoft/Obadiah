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

            TranslationInfo[] translations = new TranslationInfo[length];
            int count = 0;
            for (int i = 0; i < length; ++i) {
                if (directories[i].isFile())
                    continue;

                // removes the translation if it doesn't contain books.json
                File booksFile = new File(directories[i], BOOKS_FILE);
                if (!booksFile.exists()) {
                    Utils.removeDirectory(directories[i]);
                    continue;
                }

                // reads and parses the books.json file
                FileInputStream fis = new FileInputStream(booksFile);
                byte[] buffer = new byte[(int) booksFile.length()];
                fis.read(buffer);
                fis.close();

                translations[i] = new TranslationInfo();
                translations[i].path = directories[i].getAbsolutePath();

                JSONObject booksInfoObject = new JSONObject(new String(buffer, "UTF8"));
                translations[i].name = booksInfoObject.getString("name");

                if (booksInfoObject.has("shortName")) {
                    translations[i].shortName = booksInfoObject.getString("shortName");
                } else {
                    // old books.json doesn't have "shortName"
                    String name = translations[i].name;
                    if (name.equals("Authorized King James"))
                        translations[i].shortName = "KJV";
                    else if (name.equals("American King James"))
                        translations[i].shortName = "AKJV";
                    else if (name.equals("Basic English"))
                        translations[i].shortName = "BBE";
                    else if (name.equals("Raamattu 1938"))
                        translations[i].shortName = "PR1938";
                    else if (name.equals("Luther's Biblia"))
                        translations[i].shortName = "Lut1545";
                    else if (name.equals("Italian Deodati Bibbia") || name.equals("Italian Diodati Bibbia")) // typo
                        translations[i].shortName = "Dio";
                    else if (name.equals("Korean Revised (개역성경)"))
                        translations[i].shortName = "개역성경";
                    else if (name.equals("Reina-Valera 1569"))
                        translations[i].shortName = "RV1569";
                    else
                        translations[i].shortName = name;
                }

                JSONArray booksArray = booksInfoObject.getJSONArray("books");
                translations[i].bookName = new String[BOOK_COUNT];
                for (int j = 0; j < BOOK_COUNT; ++j)
                    translations[i].bookName[j] = booksArray.getString(j);

                ++count;
            }

            if (count > 0) {
                m_installedTranslations = new TranslationInfo[count];
                for (int i = 0, index = 0; i < length; ++i) {
                    if (translations[i] != null)
                        m_installedTranslations[index++] = translations[i];
                }
            } else {
                m_installedTranslations = null;
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
        if (m_installedTranslations == null || m_installedTranslations.length == 0) {
            m_selectedTranslation = null;
            return;
        }

        if (translation == null) {
            if (m_selectedTranslation == null)
                m_selectedTranslation = m_installedTranslations[0].path;
            return;
        }

        // tries to find the translation
        final int length = m_installedTranslations.length;
        for (int i = 0; i < length; ++i) {
            String path = m_installedTranslations[i].path;
            if (m_installedTranslations[i].path.endsWith(translation)) {
                m_selectedTranslation = path;
                return;
            }
        }

        // selects the first translation if no matching found
        if (m_selectedTranslation == null)
            m_selectedTranslation = m_installedTranslations[0].path;
    }

    public TranslationInfo selectedTranslation()
    {
        if (m_selectedTranslation == null) {
            if (m_installedTranslations == null || m_installedTranslations.length == 0) {
                return null;
            }

            m_selectedTranslation = m_installedTranslations[0].path;
            return m_installedTranslations[0];
        }

        // tries to find the selected translation
        for (TranslationInfo translationInfo : m_installedTranslations) {
            if (translationInfo.path.equals(m_selectedTranslation))
                return translationInfo;
        }

        // selects and returns the first translation if no matching found
        TranslationInfo translationInfo = m_installedTranslations[0];
        m_selectedTranslation = translationInfo.path;
        return translationInfo;
    }

    public int chapterCount(int book)
    {
        if (book < 0 || book >= BOOK_COUNT)
            return 0;
        return CHAPTER_COUNT[book];
    }

    public String[] verses(int book, int chapter)
    {
        try {
            // TODO handles if the selected chapter is invalid

            String path = m_selectedTranslation + "/" + book + "-" + chapter + ".json";
            File file = new File(path);
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            fis.close();

            JSONObject jsonObject = new JSONObject(new String(buffer, "UTF8"));
            JSONArray paragraphArray = jsonObject.getJSONArray("verses");
            final int paragraphCount = paragraphArray.length();
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

    private static final int BOOK_COUNT = 66;
    private static final int[] CHAPTER_COUNT = { 50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42,
            150, 31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4, 28, 16, 24, 21, 28, 16, 16, 13, 6,
            6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22 };
    private static final String BOOKS_FILE = "books.json";

    private static BibleReader instance;

    private File m_rootDir;
    private String m_selectedTranslation;
    private TranslationInfo[] m_installedTranslations;
}
