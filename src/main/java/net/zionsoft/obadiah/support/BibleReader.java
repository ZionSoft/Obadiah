/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2013 ZionSoft
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.zionsoft.obadiah.support;

import java.io.File;
import java.io.FileInputStream;

import net.zionsoft.obadiah.bible.TranslationInfo;

import org.json.JSONArray;
import org.json.JSONObject;

// old format is used prior to 1.5.0
class BibleReader {
    BibleReader(File rootDir) {
        try {
            final File[] directories = rootDir.listFiles();
            final int length = directories.length;
            if (length == 0)
                return;

            final TranslationInfo[] translations = new TranslationInfo[length];
            int count = 0;
            for (int i = 0; i < length; ++i) {
                if (directories[i].isFile())
                    continue;

                // removes the translation if it doesn't contain books.json
                final File booksFile = new File(directories[i], BOOKS_FILE);
                if (!booksFile.exists()) {
                    Utils.removeDirectory(directories[i]);
                    continue;
                }

                // reads and parses the books.json file
                final FileInputStream fis = new FileInputStream(booksFile);
                final byte[] buffer = new byte[(int) booksFile.length()];
                fis.read(buffer);
                fis.close();

                translations[i] = new TranslationInfo();
                translations[i].path = directories[i].getAbsolutePath();

                final JSONObject booksInfoObject = new JSONObject(new String(buffer, "UTF8"));
                translations[i].name = booksInfoObject.getString("name");

                if (booksInfoObject.has("shortName")) {
                    translations[i].shortName = booksInfoObject.getString("shortName");
                } else {
                    // old books.json doesn't have "shortName"
                    final String name = translations[i].name;
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
                        // typo released over the web
                    else if (name.equals("Italian Deodati Bibbia") || name.equals("Italian Diodati Bibbia"))
                        translations[i].shortName = "Dio";
                    else if (name.equals("Korean Revised (개역성경)"))
                        translations[i].shortName = "개역성경";
                    else if (name.equals("Reina-Valera 1569"))
                        translations[i].shortName = "RV1569";
                    else
                        translations[i].shortName = name;
                }

                final JSONArray booksArray = booksInfoObject.getJSONArray("books");
                translations[i].bookNames = new String[BOOK_COUNT];
                for (int j = 0; j < BOOK_COUNT; ++j)
                    translations[i].bookNames[j] = booksArray.getString(j);

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

    TranslationInfo[] installedTranslations() {
        return m_installedTranslations;
    }

    void selectTranslation(String translation) {
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
        for (TranslationInfo installedTranslation : m_installedTranslations) {
            if (installedTranslation.path.endsWith(translation)) {
                m_selectedTranslation = installedTranslation.path;
                return;
            }
        }

        // selects the first translation if no matching found
        if (m_selectedTranslation == null)
            m_selectedTranslation = m_installedTranslations[0].path;
    }

    String[] verses(int book, int chapter) {
        try {
            final String path = m_selectedTranslation + "/" + book + "-" + chapter + ".json";
            final File file = new File(path);
            final FileInputStream fis = new FileInputStream(file);
            final byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            fis.close();

            final JSONObject jsonObject = new JSONObject(new String(buffer, "UTF8"));
            final JSONArray paragraphArray = jsonObject.getJSONArray("verses");
            final int paragraphCount = paragraphArray.length();
            final String[] paragraphs = new String[paragraphCount];
            for (int i = 0; i < paragraphCount; ++i)
                paragraphs[i] = paragraphArray.getString(i);
            return paragraphs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final int BOOK_COUNT = 66;
    private static final String BOOKS_FILE = "books.json";

    private String m_selectedTranslation;
    private TranslationInfo[] m_installedTranslations;
}
