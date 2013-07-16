package net.zionsoft.obadiah;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class TranslationReader {
    public TranslationReader(Context context) {
        super();
        m_translationsDatabaseHelper = new TranslationsDatabaseHelper(context);
        m_selectedTranslationChanged = true;
        m_bookNames = new String[BOOK_COUNT];
    }

    public void selectTranslation(String translationShortName) {
        final SQLiteDatabase db = m_translationsDatabaseHelper.getReadableDatabase();
        Cursor cursor;
        if (translationShortName != null) {
            cursor = db.query(TranslationsDatabaseHelper.TABLE_TRANSLATIONS,
                    new String[]{TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME},
                    TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME + " = ? AND "
                            + TranslationsDatabaseHelper.COLUMN_INSTALLED + " = ?", new String[]{
                    translationShortName, "1"}, null, null, null);
        } else {
            // if the given name is null, choose the first installed translation
            cursor = db.query(TranslationsDatabaseHelper.TABLE_TRANSLATIONS,
                    new String[]{TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME},
                    TranslationsDatabaseHelper.COLUMN_INSTALLED + " = ?", new String[]{"1"}, null, null, null, "1");
        }

        if (cursor == null || cursor.getCount() != 1) {
            db.close();
            throw new IllegalArgumentException();
        }

        if (translationShortName == null) {
            cursor.moveToNext();
            translationShortName = cursor.getString(cursor
                    .getColumnIndex(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME));
        }

        db.close();
        m_selectedTranslationChanged = true;
        m_selectedTranslationShortName = translationShortName;
    }

    public String selectedTranslationShortName() {
        return m_selectedTranslationShortName;
    }

    public static int bookCount() {
        return BOOK_COUNT;
    }

    public static int chapterCount(int bookIndex) {
        if (bookIndex < 0 || bookIndex >= BOOK_COUNT)
            throw new IllegalArgumentException();
        return CHAPTER_COUNT[bookIndex];
    }

    public String[] bookNames() {
        if (m_selectedTranslationShortName == null)
            return null;

        if (!m_selectedTranslationChanged)
            return m_bookNames;

        // loads the book names
        final SQLiteDatabase db = m_translationsDatabaseHelper.getReadableDatabase();
        final Cursor cursor = db.query(TranslationsDatabaseHelper.TABLE_BOOK_NAMES,
                new String[]{TranslationsDatabaseHelper.COLUMN_BOOK_NAME},
                TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME + " = ?",
                new String[]{m_selectedTranslationShortName}, null, null,
                TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + " ASC");
        if (cursor == null) {
            db.close();
            return null;
        }

        final int bookNameColumnIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_BOOK_NAME);
        int i = 0;
        while (cursor.moveToNext())
            m_bookNames[i++] = cursor.getString(bookNameColumnIndex);
        db.close();
        m_selectedTranslationChanged = false;
        return m_bookNames;
    }

    public String[] verses(int bookIndex, int chapterIndex) {
        // TODO caches the verses in memory

        if (m_selectedTranslationShortName == null)
            return null;

        if (chapterIndex < 0 || chapterIndex >= chapterCount(bookIndex))
            throw new IllegalArgumentException();

        final SQLiteDatabase db = m_translationsDatabaseHelper.getReadableDatabase();
        final Cursor cursor = db.query(m_selectedTranslationShortName,
                new String[]{TranslationsDatabaseHelper.COLUMN_TEXT}, TranslationsDatabaseHelper.COLUMN_BOOK_INDEX
                + " = ? AND " + TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX + " = ?", new String[]{
                Integer.toString(bookIndex), Integer.toString(chapterIndex)}, null, null,
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
        final String[] texts = new String[count];
        int i = 0;
        while (cursor.moveToNext())
            texts[i++] = cursor.getString(textColumnIndex);
        db.close();
        return texts;
    }

    private static final int BOOK_COUNT = 66;
    private static final int[] CHAPTER_COUNT = {50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42,
            150, 31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4, 28, 16, 24, 21, 28, 16, 16, 13, 6,
            6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22};

    private boolean m_selectedTranslationChanged;
    private String m_selectedTranslationShortName;
    private String[] m_bookNames;

    private TranslationsDatabaseHelper m_translationsDatabaseHelper;
}
