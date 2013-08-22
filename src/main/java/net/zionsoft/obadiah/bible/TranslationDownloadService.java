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

package net.zionsoft.obadiah.bible;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;

import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.util.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TranslationDownloadService extends IntentService {
    public static final String ACTION_CANCEL_DOWNLOAD
            = "net.zionsoft.obadiah.bible.TranslationDownloadService.ACTION_CANCEL_DOWNLOAD";
    public static final String ACTION_STATUS_UPDATE
            = "net.zionsoft.obadiah.bible.TranslationDownloadService.ACTION_STATUS_UPDATE";

    public static final String KEY_PROGRESS
            = "net.zionsoft.obadiah.bible.TranslationDownloadService.KEY_PROGRESS";
    public static final String KEY_STATUS
            = "net.zionsoft.obadiah.bible.TranslationDownloadService.KEY_STATUS";
    public static final String KEY_TRANSLATION
            = "net.zionsoft.obadiah.bible.TranslationDownloadService.KEY_TRANSLATION";

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_IN_PROGRESS = 1;
    public static final int STATUS_CANCELED = 2;
    public static final int STATUS_NETWORK_FAILURE = 3;
    public static final int STATUS_SERVER_FAILURE = 4;

    public TranslationDownloadService() {
        super("net.zionsoft.obadiah.bible.TranslationDownloadService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int status = STATUS_SUCCESS;
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        ZipInputStream zis = null;
        SQLiteDatabase db = null;
        try {
            mRequestCancelListener = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mCanceled = true;
                }
            };
            localBroadcastManager.registerReceiver(mRequestCancelListener,
                    new IntentFilter(ACTION_CANCEL_DOWNLOAD));

            db = new TranslationsDatabaseHelper(this).getWritableDatabase();
            db.beginTransaction();

            // creates a translation table
            final TranslationInfo translation = intent.getParcelableExtra(KEY_TRANSLATION);
            db.execSQL(String.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL);",
                    translation.shortName, TranslationsDatabaseHelper.COLUMN_BOOK_INDEX,
                    TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX,
                    TranslationsDatabaseHelper.COLUMN_VERSE_INDEX,
                    TranslationsDatabaseHelper.COLUMN_TEXT));
            db.execSQL(String.format("CREATE INDEX INDEX_%s ON %s (%s, %s, %s);",
                    translation.shortName, translation.shortName,
                    TranslationsDatabaseHelper.COLUMN_BOOK_INDEX,
                    TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX,
                    TranslationsDatabaseHelper.COLUMN_VERSE_INDEX));

            zis = new ZipInputStream(NetworkHelper.get(String.
                    format("downloadTranslation?blobKey=%s",
                            URLEncoder.encode(translation.blobKey, "UTF-8"))));

            final byte buffer[] = new byte[BUFFER_LENGTH];
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            final ContentValues versesValues = new ContentValues(4);
            int downloaded = 0;
            int read;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (mCanceled) {
                    status = STATUS_CANCELED;
                    return;
                }

                os.reset();
                while ((read = zis.read(buffer, 0, BUFFER_LENGTH)) != -1)
                    os.write(buffer, 0, read);
                final byte[] bytes = os.toByteArray();
                String fileName = entry.getName();
                fileName = fileName.substring(0, fileName.length() - 5); // removes the trailing ".json"
                if (fileName.equals("books")) {
                    // writes the book names table

                    final ContentValues bookNamesValues = new ContentValues(3);
                    bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME,
                            translation.shortName);

                    final JSONObject booksInfoObject = new JSONObject(new String(bytes, "UTF8"));
                    final JSONArray booksArray = booksInfoObject.getJSONArray("books");
                    for (int i = 0; i < TranslationReader.bookCount(); ++i) {
                        bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, i);
                        bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_NAME,
                                booksArray.getString(i));
                        db.insert(TranslationsDatabaseHelper.TABLE_BOOK_NAMES,
                                null, bookNamesValues);
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
                        versesValues.put(TranslationsDatabaseHelper.COLUMN_TEXT,
                                paragraphArray.getString(verseIndex));
                        db.insert(translation.shortName, null, versesValues);
                    }
                }

                // broadcasts progress
                localBroadcastManager.sendBroadcast(new Intent(ACTION_STATUS_UPDATE)
                        .putExtra(KEY_STATUS, STATUS_IN_PROGRESS)
                        .putExtra(KEY_PROGRESS, ++downloaded / 12));
            }

            // marks as "installed"
            final ContentValues values = new ContentValues(1);
            values.put(TranslationsDatabaseHelper.COLUMN_VALUE, Boolean.toString(true));
            db.update(TranslationsDatabaseHelper.TABLE_TRANSLATION_LIST, values,
                    String.format("%s = ? AND %s = ?",
                            TranslationsDatabaseHelper.COLUMN_TRANSLATION_ID,
                            TranslationsDatabaseHelper.COLUMN_KEY),
                    new String[]{Long.toString(translation.uniqueId),
                            TranslationsDatabaseHelper.KEY_INSTALLED});

            // sets as selected translation
            getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE).edit()
                    .putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, translation.shortName)
                    .commit();

            db.setTransactionSuccessful();
        } catch (IOException e) {
            // network failure
            status = STATUS_NETWORK_FAILURE;
        } catch (JSONException e) {
            // malformed server response
            status = STATUS_SERVER_FAILURE;
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                    // TODO
                }
            }
            if (db != null) {
                db.endTransaction();
                db.close();
            }

            getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putBoolean(Constants.PREF_KEY_DOWNLOADING_TRANSLATION, false).commit();

            localBroadcastManager.unregisterReceiver(mRequestCancelListener);
            localBroadcastManager.sendBroadcast(new Intent(ACTION_STATUS_UPDATE)
                    .putExtra(KEY_STATUS, status));
        }
    }

    private static final int BUFFER_LENGTH = 2048;

    private boolean mCanceled = false;
    private BroadcastReceiver mRequestCancelListener;
}
