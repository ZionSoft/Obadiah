package net.zionsoft.obadiah;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class TranslationManager
{
    public TranslationManager(Context context)
    {
        super();
        m_context = context;
        m_translationsDatabaseHelper = new TranslationsDatabaseHelper(context);
    }

    public void addTranslations(TranslationInfo[] translations)
    {
        if (translations == null || translations.length == 0)
            throw new IllegalArgumentException();

        final TranslationInfo[] existingTranslations = translations();
        final ContentValues values = new ContentValues(5);
        final SQLiteDatabase db = m_translationsDatabaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (TranslationInfo translationInfo : translations) {
                if (existingTranslations != null) {
                    int i = 0;
                    for (; i < existingTranslations.length; ++i) {
                        if (translationInfo.shortName.equals(existingTranslations[i].shortName))
                            break;
                    }
                    if (i < existingTranslations.length)
                        continue;
                }

                values.put(TranslationsDatabaseHelper.COLUMN_INSTALLED, 0);
                values.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_NAME, translationInfo.name);
                values.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME, translationInfo.shortName);
                values.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, translationInfo.language);
                values.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, translationInfo.size);
                db.insert(TranslationsDatabaseHelper.TABLE_TRANSLATIONS, null, values);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public boolean installTranslation(TranslationDownloadActivity.TranslationDownloadAsyncTask callback,
            TranslationInfo translationToDownload)
    {
        final SQLiteDatabase db = m_translationsDatabaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // creates a translation table
            db.execSQL("CREATE TABLE " + translationToDownload.shortName + " ("
                    + TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + " INTEGER NOT NULL, "
                    + TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX + " INTEGER NOT NULL, "
                    + TranslationsDatabaseHelper.COLUMN_VERSE_INDEX + " INTEGER NOT NULL, "
                    + TranslationsDatabaseHelper.COLUMN_TEXT + " TEXT NOT NULL);");
            db.execSQL("CREATE INDEX INDEX_" + translationToDownload.shortName + " ON "
                    + translationToDownload.shortName + " (" + TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + ", "
                    + TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX + ", "
                    + TranslationsDatabaseHelper.COLUMN_VERSE_INDEX + ");");

            // gets the data and writes to table
            final URL url = new URL(TranslationDownloadActivity.BASE_URL
                    + URLEncoder.encode(translationToDownload.shortName, "UTF-8") + ".zip");
            final HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(httpConnection.getInputStream()));

            final byte buffer[] = new byte[BUFFER_LENGTH];
            final ContentValues versesValues = new ContentValues(4);
            int read = -1;
            int downloaded = 0;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (callback.isCancelled())
                    break;

                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                while ((read = zis.read(buffer, 0, BUFFER_LENGTH)) != -1)
                    os.write(buffer, 0, read);
                final byte[] bytes = os.toByteArray();

                String fileName = entry.getName();
                fileName = fileName.substring(0, fileName.length() - 5); // removes the trailing ".json"
                if (fileName.equals("books")) {
                    // writes the book names table
                    final ContentValues bookNamesValues = new ContentValues(3);
                    bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME,
                            translationToDownload.shortName);

                    final JSONObject booksInfoObject = new JSONObject(new String(bytes, "UTF8"));
                    final JSONArray booksArray = booksInfoObject.getJSONArray("books");
                    for (int i = 0; i < 66; ++i) { // TODO gets rid of magic number
                        bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, i);
                        bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_NAME, booksArray.getString(i));
                        db.insert(TranslationsDatabaseHelper.TABLE_BOOK_NAMES, null, bookNamesValues);
                    }
                } else {
                    // writes the verses
                    final String[] parts = fileName.split("-");
                    final int bookIndex = Integer.parseInt(parts[0]);
                    final int chapterIndex = Integer.parseInt(parts[1]);
                    versesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, bookIndex);
                    versesValues.put(TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX, chapterIndex);

                    final JSONObject jsonObject = new JSONObject(new String(bytes, "UTF8"));
                    final JSONArray paragraphArray = jsonObject.getJSONArray("verses");
                    final int paragraphCount = paragraphArray.length();
                    for (int verseIndex = 0; verseIndex < paragraphCount; ++verseIndex) {
                        versesValues.put(TranslationsDatabaseHelper.COLUMN_VERSE_INDEX, verseIndex);
                        versesValues.put(TranslationsDatabaseHelper.COLUMN_TEXT, paragraphArray.getString(verseIndex));
                        db.insert(translationToDownload.shortName, null, versesValues);
                    }
                }

                // notifies the progress
                callback.updateProgress(++downloaded / 12);
            }
            zis.close();

            // sets as installed
            final ContentValues translationInfoValues = new ContentValues(1);
            translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_INSTALLED, 1);
            db.update(TranslationsDatabaseHelper.TABLE_TRANSLATIONS, translationInfoValues,
                    TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME + " = ?",
                    new String[] { translationToDownload.shortName });

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
            db.close();
        }
        return true;
    }

    public void removeTranslation(String translationShortName)
    {
        if (translationShortName == null)
            throw new IllegalArgumentException();

        final SQLiteDatabase db = m_translationsDatabaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.execSQL("DROP TABLE IF EXISTS " + translationShortName);

            final ContentValues values = new ContentValues(1);
            values.put(TranslationsDatabaseHelper.COLUMN_INSTALLED, 0);
            db.update(TranslationsDatabaseHelper.TABLE_TRANSLATIONS, values,
                    TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME + " = ?",
                    new String[] { translationShortName });

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // translations before version 1.5.0 uses the old format
    public void convertFromOldFormat()
    {
        final BibleReader oldReader = new BibleReader(m_context.getFilesDir());
        final TranslationInfo[] installedTranslations = oldReader.installedTranslations();
        if (installedTranslations == null || installedTranslations.length == 0)
            return;

        final SQLiteDatabase db = m_translationsDatabaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final ContentValues versesValues = new ContentValues(4);
            final ContentValues bookNamesValues = new ContentValues(3);
            final ContentValues translationInfoValues = new ContentValues(5);
            translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_INSTALLED, 1);
            for (TranslationInfo translationInfo : installedTranslations) {
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
                            translationInfo.bookName[bookIndex]);
                    db.insert(TranslationsDatabaseHelper.TABLE_BOOK_NAMES, null, bookNamesValues);
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

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public TranslationInfo[] translations()
    {
        final SQLiteDatabase db = m_translationsDatabaseHelper.getReadableDatabase();
        final String[] columns = new String[] { TranslationsDatabaseHelper.COLUMN_TRANSLATION_NAME,
                TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME, TranslationsDatabaseHelper.COLUMN_LANGUAGE,
                TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, TranslationsDatabaseHelper.COLUMN_INSTALLED };
        final Cursor cursor = db.query(TranslationsDatabaseHelper.TABLE_TRANSLATIONS, columns, null, null, null, null,
                null);
        if (cursor == null) {
            db.close();
            return null;
        }
        final int count = cursor.getCount();
        if (count == 0) {
            db.close();
            return null;
        }

        final int languageColumnIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_LANGUAGE);
        final int translationNameColumnIndex = cursor
                .getColumnIndex(TranslationsDatabaseHelper.COLUMN_TRANSLATION_NAME);
        final int translationShortNameColumnIndex = cursor
                .getColumnIndex(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME);
        final int downloadSizeColumnIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE);
        final int installedColumnIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_INSTALLED);
        final TranslationInfo[] translations = new TranslationInfo[count];
        int i = 0;
        while (cursor.moveToNext()) {
            final TranslationInfo translationinfo = new TranslationInfo();
            translationinfo.installed = (cursor.getInt(installedColumnIndex) == 1) ? true : false;
            translationinfo.size = cursor.getInt(downloadSizeColumnIndex);
            translationinfo.shortName = cursor.getString(translationShortNameColumnIndex);
            translationinfo.name = cursor.getString(translationNameColumnIndex);
            translationinfo.language = cursor.getString(languageColumnIndex);
            translationinfo.path = translationinfo.shortName;
            translations[i++] = translationinfo;
        }
        db.close();
        return translations;
    }

    private static final int BUFFER_LENGTH = 2048;

    private Context m_context;
    private TranslationsDatabaseHelper m_translationsDatabaseHelper;
}
