package net.zionsoft.obadiah;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class TranslationReader
{
    public TranslationReader(Context context)
    {
        super();
        m_translationsDatabaseHelper = new TranslationsDatabaseHelper(context);
    }

    public void selectTranslation(String translationShortName)
    {
        if (translationShortName == null)
            throw new IllegalArgumentException();

        // versions before 1.5.0 uses path instead of translation short name as the key
        if (translationShortName.equals("danske-bibel1871"))
            translationShortName = "DA1871";
        else if (translationShortName.equals("authorized-king-james"))
            translationShortName = "KJV";
        else if (translationShortName.equals("american-king-james"))
            translationShortName = "AKJV";
        else if (translationShortName.equals("basic-english"))
            translationShortName = "BBE";
        else if (translationShortName.equals("esv"))
            translationShortName = "ESV";
        else if (translationShortName.equals("raamattu1938"))
            translationShortName = "PR1938";
        else if (translationShortName.equals("fre-segond"))
            translationShortName = "FreSegond";
        else if (translationShortName.equals("darby-elb1905"))
            translationShortName = "Elb1905";
        else if (translationShortName.equals("luther-biblia"))
            translationShortName = "Lut1545";
        else if (translationShortName.equals("italian-diodati-bibbia"))
            translationShortName = "Dio";
        else if (translationShortName.equals("korean-revised"))
            translationShortName = "개역성경";
        else if (translationShortName.equals("biblia-almeida-recebida"))
            translationShortName = "PorAR";
        else if (translationShortName.equals("reina-valera1569"))
            translationShortName = "RV1569";
        else if (translationShortName.equals("chinese-union-traditional"))
            translationShortName = "華語和合本";
        else if (translationShortName.equals("chinese-union-simplified"))
            translationShortName = "中文和合本";
        else if (translationShortName.equals("chinese-new-version-traditional"))
            translationShortName = "華語新譯本";
        else if (translationShortName.equals("chinese-new-version-simplified"))
            translationShortName = "中文新译本";

        SQLiteDatabase db = m_translationsDatabaseHelper.getReadableDatabase();
        Cursor cursor = db.query(TranslationsDatabaseHelper.TABLE_TRANSLATIONS,
                new String[] { TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME },
                TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME + " = ? AND "
                        + TranslationsDatabaseHelper.COLUMN_INSTALLED + " = ?", new String[] { translationShortName,
                        "1" }, null, null, null);
        if (cursor == null || cursor.getCount() != 1) {
            db.close();
            throw new IllegalArgumentException();
        }

        db.close();
        m_selectedTranslationShortName = translationShortName;
    }

    public int bookCount()
    {
        return BOOK_COUNT;
    }

    public int chapterCount(int bookIndex)
    {
        if (bookIndex < 0 || bookIndex >= BOOK_COUNT)
            throw new IllegalArgumentException();
        return CHAPTER_COUNT[bookIndex];
    }

    public String[] bookNames()
    {
        if (m_selectedTranslationShortName == null)
            throw new IllegalArgumentException();

        SQLiteDatabase db = m_translationsDatabaseHelper.getReadableDatabase();
        Cursor cursor = db.query(TranslationsDatabaseHelper.TABLE_BOOK_NAMES,
                new String[] { TranslationsDatabaseHelper.COLUMN_BOOK_NAME },
                TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME + " = ?",
                new String[] { m_selectedTranslationShortName }, null, null,
                TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + " ASC");
        if (cursor == null) {
            db.close();
            return null;
        }
        final int count = cursor.getCount();
        if (count != 66) { // TODO error handling
            db.close();
            return null;
        }

        final int bookNameColumnIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_BOOK_NAME);
        String[] bookNames = new String[count];
        int i = 0;
        while (cursor.moveToNext())
            bookNames[i++] = cursor.getString(bookNameColumnIndex);
        db.close();
        return bookNames;
    }

    public String[] verses(int bookIndex, int chapterIndex)
    {
        if (m_selectedTranslationShortName == null || chapterIndex < 0 || chapterIndex >= chapterCount(bookIndex))
            throw new IllegalArgumentException();

        SQLiteDatabase db = m_translationsDatabaseHelper.getReadableDatabase();
        Cursor cursor = db.query(m_selectedTranslationShortName,
                new String[] { TranslationsDatabaseHelper.COLUMN_TEXT }, TranslationsDatabaseHelper.COLUMN_BOOK_INDEX
                        + " = ? AND " + TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX + " = ?", new String[] {
                        Integer.toString(bookIndex), Integer.toString(chapterIndex) }, null, null,
                TranslationsDatabaseHelper.COLUMN_VERSE_INDEX + " ASC");
        if (cursor == null) {
            db.close();
            return null;
        }
        final int count = cursor.getCount();
        if (count == 0) {
            db.close();
            return null;
        }

        final int textColumnIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_TEXT);
        String[] texts = new String[count];
        int i = 0;
        while (cursor.moveToNext())
            texts[i++] = cursor.getString(textColumnIndex);
        db.close();
        return texts;
    }

    private static final int BOOK_COUNT = 66;
    private static final int[] CHAPTER_COUNT = { 50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42,
            150, 31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4, 28, 16, 24, 21, 28, 16, 16, 13, 6,
            6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22 };

    private String m_selectedTranslationShortName;
    private TranslationsDatabaseHelper m_translationsDatabaseHelper;
}
