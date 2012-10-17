package net.zionsoft.obadiah;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TranslationsDatabaseHelper extends SQLiteOpenHelper
{
    public TranslationsDatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db)
    {
        db.beginTransaction();
        try {
            // creates the translations table
            db.execSQL("CREATE TABLE " + TABLE_TRANSLATIONS + " (" + COLUMN_TRANSLATION_NAME + " TEXT NOT NULL, "
                    + COLUMN_TRANSLATION_SHORTNAME + " TEXT UNIQUE NOT NULL, " + COLUMN_LANGUAGE + " TEXT NOT NULL, "
                    + COLUMN_DOWNLOAD_SIZE + " INTEGER NOT NULL, " + COLUMN_INSTALLED + " INTEGER NOT NULL);");

            // creates the books name table
            db.execSQL("CREATE TABLE " + TABLE_BOOK_NAMES + " (" + COLUMN_TRANSLATION_SHORTNAME + " TEXT NOT NULL, "
                    + COLUMN_BOOK_INDEX + " INTEGER NOT NULL, " + COLUMN_BOOK_NAME + " TEXT NOT NULL);");
            db.execSQL("CREATE INDEX " + TABLE_BOOK_NAMES + " ON " + TABLE_BOOK_NAMES + " ("
                    + COLUMN_TRANSLATION_SHORTNAME + ");");

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        // does nothing
    }

    public static final String TABLE_BOOK_NAMES = "TABLE_BOOK_NAMES";
    public static final String TABLE_TRANSLATIONS = "TABLE_TRANSLATIONS";
    public static final String COLUMN_TRANSLATION_NAME = "COLUMN_TRANSLATION_NAME";
    public static final String COLUMN_TRANSLATION_SHORTNAME = "COLUMN_TRANSLATION_SHORTNAME";
    public static final String COLUMN_LANGUAGE = "COLUMN_LANGUAGE";
    public static final String COLUMN_DOWNLOAD_SIZE = "COLUMN_DOWNLOAD_SIZE";
    public static final String COLUMN_INSTALLED = "COLUMN_INSTALLED";
    public static final String COLUMN_BOOK_NAME = "COLUMN_BOOK_NAME";
    public static final String COLUMN_BOOK_INDEX = "COLUMN_BOOK_INDEX";
    public static final String COLUMN_CHAPTER_INDEX = "COLUMN_CHAPTER_INDEX";
    public static final String COLUMN_VERSE_INDEX = "COLUMN_VERSE_INDEX";
    public static final String COLUMN_TEXT = "COLUMN_TEXT";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "DB_OBADIAH";
}
