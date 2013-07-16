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

import net.zionsoft.obadiah.BookSelectionActivity;
import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.bible.TranslationManager;
import net.zionsoft.obadiah.bible.TranslationReader;
import net.zionsoft.obadiah.bible.TranslationsDatabaseHelper;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

public class UpgradeAsyncTask extends AsyncTask<Void, Integer, Void> {
    public UpgradeAsyncTask(BookSelectionActivity bookSelectionActivity) {
        super();
        m_bookSelectionActivity = bookSelectionActivity;
    }

    protected void onPreExecute() {
        // running in the main thread

        m_progressDialog = new ProgressDialog(m_bookSelectionActivity);
        m_progressDialog.setCancelable(false);
        m_progressDialog.setMessage(m_bookSelectionActivity.getText(R.string.text_initializing));
        m_progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        m_progressDialog.setMax(100);
        m_progressDialog.setProgress(0);
        m_progressDialog.show();
    }

    protected Void doInBackground(Void... params) {
        // running in the worker thread

        convertTranslations();
        publishProgress(98);

        convertSettings();
        publishProgress(99);

        // sets the application version
        final SharedPreferences preferences = m_bookSelectionActivity.getSharedPreferences(Constants.SETTING_KEY,
                Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(Constants.CURRENT_APPLICATION_VERSION_SETTING_KEY, Constants.CURRENT_APPLICATION_VERSION);
        editor.commit();

        publishProgress(100);

        return null;
    }

    protected void onProgressUpdate(Integer... progress) {
        // running in the main thread

        m_progressDialog.setProgress(progress[0]);
    }

    protected void onPostExecute(Void result) {
        // running in the main thread

        m_bookSelectionActivity.onUpgradeFinished();
        m_progressDialog.dismiss();
    }

    private void convertTranslations() {
        // old translations format is used prior to 1.5.0

        final File rootDir = m_bookSelectionActivity.getFilesDir();
        final BibleReader oldReader = new BibleReader(rootDir);
        final TranslationInfo[] installedTranslations = oldReader.installedTranslations();
        if (installedTranslations == null || installedTranslations.length == 0)
            return;

        final SQLiteDatabase db = new TranslationsDatabaseHelper(m_bookSelectionActivity).getWritableDatabase();
        db.beginTransaction();
        try {
            final ContentValues versesValues = new ContentValues(4);
            final ContentValues bookNamesValues = new ContentValues(3);
            final ContentValues translationInfoValues = new ContentValues(5);
            translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_INSTALLED, 1);

            final double progressDelta = 1.36 / installedTranslations.length;
            double currentProgress = 1.0;
            for (TranslationInfo translationInfo : installedTranslations) {
                publishProgress((int) currentProgress);

                // creates a translation table
                db.execSQL("CREATE TABLE " + translationInfo.shortName + " ("
                        + TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + " INTEGER NOT NULL, "
                        + TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX + " INTEGER NOT NULL, "
                        + TranslationsDatabaseHelper.COLUMN_VERSE_INDEX + " INTEGER NOT NULL, "
                        + TranslationsDatabaseHelper.COLUMN_TEXT + " TEXT NOT NULL);");
                db.execSQL("CREATE INDEX INDEX_" + translationInfo.shortName + " ON " + translationInfo.shortName
                        + " (" + TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + ", "
                        + TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX + ", "
                        + TranslationsDatabaseHelper.COLUMN_VERSE_INDEX + ");");

                bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME, translationInfo.shortName);

                oldReader.selectTranslation(translationInfo.path);
                for (int bookIndex = 0; bookIndex < 66; ++bookIndex) {
                    // writes verses
                    final int chapterCount = TranslationReader.chapterCount(bookIndex);
                    for (int chapterIndex = 0; chapterIndex < chapterCount; ++chapterIndex) {
                        String[] texts = oldReader.verses(bookIndex, chapterIndex);
                        int verseIndex = 0;
                        for (String text : texts) {
                            versesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, bookIndex);
                            versesValues.put(TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX, chapterIndex);
                            versesValues.put(TranslationsDatabaseHelper.COLUMN_VERSE_INDEX, verseIndex++);
                            versesValues.put(TranslationsDatabaseHelper.COLUMN_TEXT, text);
                            db.insert(translationInfo.shortName, null, versesValues);
                        }
                    }

                    // writes book name
                    bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, bookIndex);
                    bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_NAME,
                            translationInfo.bookNames[bookIndex]);
                    db.insert(TranslationsDatabaseHelper.TABLE_BOOK_NAMES, null, bookNamesValues);

                    currentProgress += progressDelta;
                    publishProgress((int) currentProgress);
                }

                // adds to the translations table
                translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_NAME, translationInfo.name);
                translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME,
                        translationInfo.shortName);

                if (translationInfo.shortName.equals("DA1871")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1843);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Dansk");
                } else if (translationInfo.shortName.equals("KJV")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1817);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "English");
                } else if (translationInfo.shortName.equals("AKJV")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1799);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "English");
                } else if (translationInfo.shortName.equals("BBE")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1826);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "English");
                } else if (translationInfo.shortName.equals("ESV")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1780);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "English");
                } else if (translationInfo.shortName.equals("PR1938")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1950);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Suomi");
                } else if (translationInfo.shortName.equals("FreSegond")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1972);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Français");
                } else if (translationInfo.shortName.equals("Elb1905")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1990);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Deutsche");
                } else if (translationInfo.shortName.equals("Lut1545")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1880);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Deutsche");
                } else if (translationInfo.shortName.equals("Dio")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, "Italiano");
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, 1843);
                } else if (translationInfo.shortName.equals("개역성경")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1923);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "한국인");
                } else if (translationInfo.shortName.equals("PorAR")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1950);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Português");
                } else if (translationInfo.shortName.equals("RV1569")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1855);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Español");
                } else if (translationInfo.shortName.equals("華語和合本")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1772);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "正體中文");
                } else if (translationInfo.shortName.equals("中文和合本")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1739);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "简体中文");
                } else if (translationInfo.shortName.equals("華語新譯本")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1874);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "正體中文");
                } else if (translationInfo.shortName.equals("中文新译本")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1877);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "简体中文");
                }

                db.insert(TranslationsDatabaseHelper.TABLE_TRANSLATIONS, null, translationInfoValues);
            }

            publishProgress(91);
            db.setTransactionSuccessful();
            Utils.removeDirectory(rootDir);
        } finally {
            publishProgress(92);
            db.endTransaction();
            db.close();
        }
    }

    private void convertSettings() {
        // old settings format is used prior to 1.5.0

        final SharedPreferences preferences = m_bookSelectionActivity.getSharedPreferences(Constants.SETTING_KEY,
                Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        String selectedTranslation;
        try {
            selectedTranslation = preferences.getString("selectedTranslation", null);
            if (selectedTranslation != null) {
                if (selectedTranslation.equals("danske-bibel1871"))
                    selectedTranslation = "DA1871";
                else if (selectedTranslation.equals("authorized-king-james"))
                    selectedTranslation = "KJV";
                else if (selectedTranslation.equals("american-king-james"))
                    selectedTranslation = "AKJV";
                else if (selectedTranslation.equals("basic-english"))
                    selectedTranslation = "BBE";
                else if (selectedTranslation.equals("esv"))
                    selectedTranslation = "ESV";
                else if (selectedTranslation.equals("raamattu1938"))
                    selectedTranslation = "PR1938";
                else if (selectedTranslation.equals("fre-segond"))
                    selectedTranslation = "FreSegond";
                else if (selectedTranslation.equals("darby-elb1905"))
                    selectedTranslation = "Elb1905";
                else if (selectedTranslation.equals("luther-biblia"))
                    selectedTranslation = "Lut1545";
                else if (selectedTranslation.equals("italian-diodati-bibbia"))
                    selectedTranslation = "Dio";
                else if (selectedTranslation.equals("korean-revised"))
                    selectedTranslation = "개역성경";
                else if (selectedTranslation.equals("biblia-almeida-recebida"))
                    selectedTranslation = "PorAR";
                else if (selectedTranslation.equals("reina-valera1569"))
                    selectedTranslation = "RV1569";
                else if (selectedTranslation.equals("chinese-union-traditional"))
                    selectedTranslation = "華語和合本";
                else if (selectedTranslation.equals("chinese-union-simplified"))
                    selectedTranslation = "中文和合本";
                else if (selectedTranslation.equals("chinese-new-version-traditional"))
                    selectedTranslation = "華語新譯本";
                else if (selectedTranslation.equals("chinese-new-version-simplified"))
                    selectedTranslation = "中文新译本";
            }
        } catch (ClassCastException e) {
            // the value is an integer prior to 1.2.0
            final int selected = preferences.getInt("selectedTranslation", 0);
            if (selected == 1)
                selectedTranslation = "中文和合本";
            else
                selectedTranslation = "KJV";
        }
        final TranslationInfo[] translations = new TranslationManager(m_bookSelectionActivity).translations();
        if (translations != null) {
            for (TranslationInfo translation : translations) {
                if (translation.shortName.equals(selectedTranslation))
                    editor.putString(Constants.CURRENT_TRANSLATION_SETTING_KEY, selectedTranslation);
            }
        }
        editor.remove("selectedTranslation");

        editor.commit();
    }

    private BookSelectionActivity m_bookSelectionActivity;
    private ProgressDialog m_progressDialog;
}
