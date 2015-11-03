/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2015 ZionSoft
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

package net.zionsoft.obadiah.model.translations;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.DatabaseHelper;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class TranslationManager {
    @Inject
    DatabaseHelper databaseHelper;

    @Inject
    public TranslationManager(Context context) {
        App.get(context).getInjectionComponent().inject(this);
    }

    @NonNull
    public List<TranslationInfo> loadTranslations() {
        SQLiteDatabase db = null;
        try {
            db = databaseHelper.openDatabase();
            if (db != null) {
                return TranslationHelper.sortByLocale(TranslationHelper.getTranslations(db));
            } else {
                Analytics.trackException("Failed to open database.");
                return Collections.emptyList();
            }
        } finally {
            if (db != null) {
                databaseHelper.closeDatabase();
            }
        }
    }

    @NonNull
    public List<String> loadDownloadedTranslations() {
        SQLiteDatabase db = null;
        try {
            db = databaseHelper.openDatabase();
            if (db != null) {
                return TranslationHelper.getDownloadedTranslationShortNames(db);
            } else {
                Analytics.trackException("Failed to open database.");
                return Collections.emptyList();
            }
        } finally {
            if (db != null) {
                databaseHelper.closeDatabase();
            }
        }
    }

    public void saveTranslations(List<TranslationInfo> translations) {
        SQLiteDatabase db = null;
        try {
            db = databaseHelper.openDatabase();
            if (db != null) {
                TranslationHelper.saveTranslations(db, translations);
            } else {
                Analytics.trackException("Failed to open database.");
            }
        } finally {
            if (db != null) {
                databaseHelper.closeDatabase();
            }
        }
    }

    public boolean removeTranslation(TranslationInfo translation) {
        final boolean removed = removeTranslation(translation.shortName);
        Analytics.trackTranslationRemoval(translation.shortName, removed);
        return removed;
    }

    private boolean removeTranslation(String translationShortName) {
        SQLiteDatabase db = null;
        try {
            db = databaseHelper.openDatabase();
            if (db == null) {
                Analytics.trackException("Failed to open database.");
                return false;
            }
            db.beginTransaction();
            TranslationHelper.removeTranslation(db, translationShortName);
            db.setTransactionSuccessful();

            return true;
        } finally {
            if (db != null) {
                if (db.inTransaction()) {
                    db.endTransaction();
                }
                databaseHelper.closeDatabase();
            }
        }
    }
}
