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

package net.zionsoft.obadiah.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.squareup.moshi.Moshi;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.biblereading.BibleReadingActivity;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.crash.Crash;
import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.utils.TextFormatter;

import java.util.Map;

import javax.inject.Inject;

public class PushNotificationReceiver extends FirebaseMessagingService {
    private static final int NOTIFICATION_ID_VERSE = 1;

    private static final String MESSAGE_TYPE_VERSE = "verse";

    @Inject
    Moshi moshi;

    @Inject
    BibleReadingModel bibleReadingModel;

    @Inject
    Settings settings;

    @Override
    public void onMessageReceived(RemoteMessage message) {
        App.getComponent().inject(this);

        final Map<String, String> data = message.getData();
        if (data == null || data.size() == 0) {
            return;
        }

        final String messageType = data.get("type");
        final String messageAttrs = data.get("attrs");

        final NotificationCompat.Builder builder = buildBasicNotificationBuilder(this);
        final int notificationId;
        if (MESSAGE_TYPE_VERSE.equals(messageType)) {
            notificationId = NOTIFICATION_ID_VERSE;
            if (!settings.isDailyVerseOn()) {
                return;
            }

            final PushAttrVerseIndex verseIndex = prepareForVerse(builder, messageType, messageAttrs);
            if (verseIndex == null) {
                return;
            }

            final Bundle params = new Bundle();
            params.putString(Analytics.PARAM_ITEM_ID, verseIndex.book + "-" + verseIndex.chapter + "-" + verseIndex.verse);
            Analytics.logEvent(Analytics.EVENT_DAILY_VERSE_SHOWN, params);
        } else {
            return;
        }

        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(notificationId, builder.build());
    }

    @NonNull
    private static NotificationCompat.Builder buildBasicNotificationBuilder(Context context) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(context, R.color.blue));
        } else {
            builder.setSmallIcon(R.drawable.ic_launcher);
        }
        return builder;
    }

    @Nullable
    private PushAttrVerseIndex prepareForVerse(NotificationCompat.Builder builder,
                                               String messageType, String messageAttrs) {
        final String translationShortName = bibleReadingModel.loadCurrentTranslation();
        if (TextUtils.isEmpty(translationShortName)) {
            return null;
        }

        try {
            final PushAttrVerseIndex verseIndex = moshi.adapter(PushAttrVerseIndex.class).fromJson(messageAttrs);
            final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    BibleReadingActivity.newStartReorderToTopIntent(this, messageType, verseIndex.toVerseIndex()),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            final Verse verse = bibleReadingModel
                    .loadVerse(translationShortName, verseIndex.book, verseIndex.chapter, verseIndex.verse)
                    .toBlocking().value();
            builder.setContentIntent(pendingIntent)
                    .setContentTitle(TextFormatter.format("%s, %d:%d",
                            verse.text.bookName, verseIndex.chapter + 1, verseIndex.verse + 1))
                    .setContentText(verse.text.text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(verse.text.text));

            return verseIndex;
        } catch (Exception e) {
            Crash.report(e);
            return null;
        }
    }
}
