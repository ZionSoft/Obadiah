/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2017 ZionSoft
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

package net.zionsoft.obadiah.model.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 7;
    private static final String DATABASE_NAME = "DB_OBADIAH";

    @Inject
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            MetadataTableHelper.createTable(db);
            TranslationsTableHelper.createTable(db);
            BookNamesTableHelper.createTable(db);
            ReadingProgressTableHelper.createTable(db);
            BookmarkTableHelper.createTable(db);
            NoteTableHelper.createTable(db);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            // version 2 introduced in 1.7.0
            if (oldVersion < 2) {
                db.execSQL("DROP TABLE IF EXISTS TABLE_TRANSLATIONS");
            }

            // version 3 introduced in 1.8.0
            if (oldVersion < 3) {
                db.execSQL("DROP TABLE IF EXISTS TABLE_TRANSLATION_LIST");
            }

            // version 4 introduced in 1.8.2
            if (oldVersion < 4) {
                ReadingProgressTableHelper.createTable(db);
            }

            // version 5 introduced in 1.9.0
            if (oldVersion < 5) {
                MetadataTableHelper.createTable(db);
            }

            // version 6 introduced in 1.13.0
            if (oldVersion < 6) {
                TranslationsTableHelper.createTable(db);
            }

            // version 7 introduced in 1.14.0
            if (oldVersion < 7) {
                BookmarkTableHelper.createTable(db);
                NoteTableHelper.createTable(db);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    public SQLiteDatabase getDatabase() {
        return getWritableDatabase();
    }
}
