/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2014 ZionSoft
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

package net.zionsoft.obadiah.model.notification;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.crashlytics.android.Crashlytics;

import net.zionsoft.obadiah.BookSelectionActivity;
import net.zionsoft.obadiah.Constants;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.Verse;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.translations.TranslationHelper;

import org.json.JSONObject;

public class PushNotificationHandler extends IntentService {
    private static final String KEY_EXTRAS = "net.zionsoft.obadiah.model.notification.PushNotificationHandler.KEY_EXTRAS";

    public static Intent newStartIntent(Context context, Bundle extras) {
        return new Intent(context, PushNotificationHandler.class)
                .putExtra(KEY_EXTRAS, extras);
    }

    private static final int NOTIFICATION_ID_VERSE = 1;

    private static final String MESSAGE_TYPE_VERSE = "verse";

    public PushNotificationHandler() {
        super("net.zionsoft.obadiah.model.notification.PushNotificationHandler");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final Bundle extras = intent.getBundleExtra(KEY_EXTRAS);
        final String messageType = extras.getString("type");
        final String messageAttrs = extras.getString("attrs");

        final NotificationCompat.Builder builder = buildBasicNotificationBuilder(this, messageType);
        final int notificationId;
        if (MESSAGE_TYPE_VERSE.equals(messageType)) {
            notificationId = NOTIFICATION_ID_VERSE;
            if (!prepareForVerse(this, builder, messageType, messageAttrs)) {
                return;
            }
        } else {
            Analytics.trackNotificationEvent("unknown_message_type", messageType);
            return;
        }

        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(notificationId, builder.build());

        Analytics.trackNotificationEvent("notification_shown", messageType);
    }

    @NonNull
    private static NotificationCompat.Builder buildBasicNotificationBuilder(Context context, String messageType) {
        return new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setSmallIcon(R.drawable.ic_launcher)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setDeleteIntent(PendingIntent.getBroadcast(context, 0,
                        PushDismissedReceiver.newStartIntent(context, messageType),
                        PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private static boolean prepareForVerse(Context context, NotificationCompat.Builder builder,
                                           String messageType, String messageAttrs) {
        final SharedPreferences preferences
                = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);

        final String translationShortName
                = preferences.getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);
        if (translationShortName == null) {
            return false;
        }

        SQLiteDatabase db = null;
        try {
            final JSONObject jsonObject = new JSONObject(messageAttrs);
            final int bookIndex = jsonObject.getInt("book");
            final int chapterIndex = jsonObject.getInt("chapter");
            final int verseIndex = jsonObject.getInt("verse");

            db = DatabaseHelper.openDatabase();
            final String bookName = TranslationHelper.getBookNames(db, translationShortName).get(bookIndex);
            final Verse verse = TranslationHelper.getVerse(db, translationShortName, bookName,
                    bookIndex, chapterIndex, verseIndex);
            if (verse == null) {
                throw new IllegalArgumentException("Invalid push message attrs: " + messageAttrs);
            }

            builder.setContentIntent(PendingIntent.getActivity(context, 0,
                    BookSelectionActivity.newStartReorderToTopIntent(context, messageType, bookIndex, chapterIndex, verseIndex),
                    PendingIntent.FLAG_UPDATE_CURRENT))
                    .setContentTitle(String.format("%s, %d:%d", bookName, chapterIndex + 1, verseIndex + 1))
                    .setContentText(verse.verseText)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(verse.verseText));
        } catch (Exception e) {
            Crashlytics.logException(e);
            return false;
        } finally {
            if (db != null) {
                DatabaseHelper.closeDatabase();
            }
        }
        return true;
    }
}
