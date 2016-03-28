/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2016 ZionSoft
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

package net.zionsoft.obadiah.model.datamodel;

import android.database.sqlite.SQLiteDatabase;

import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.database.NoteTableHelper;
import net.zionsoft.obadiah.model.domain.Note;
import net.zionsoft.obadiah.model.domain.VerseIndex;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Subscriber;

@Singleton
public class NoteModel {
    private final DatabaseHelper databaseHelper;

    @Inject
    public NoteModel(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public Observable<Note> updateNote(final VerseIndex verseIndex, final String note) {
        return Observable.create(new Observable.OnSubscribe<Note>() {
            @Override
            public void call(Subscriber<? super Note> subscriber) {
                try {
                    final SQLiteDatabase db = databaseHelper.getDatabase();
                    final boolean newNote = !NoteTableHelper.hasNote(db, verseIndex);

                    final Note n = new Note(verseIndex, note, System.currentTimeMillis());
                    NoteTableHelper.saveNote(db, n);

                    if (newNote) {
                        Analytics.trackEvent(Analytics.CATEGORY_NOTES, Analytics.NOTES_ACTION_ADDED);
                    }

                    subscriber.onNext(n);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<Void> removeNote(final VerseIndex verseIndex) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    NoteTableHelper.removeNote(databaseHelper.getDatabase(), verseIndex);
                    Analytics.trackEvent(Analytics.CATEGORY_NOTES, Analytics.NOTES_ACTION_REMOVED);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<List<Note>> loadNotes(final int book, final int chapter) {
        return Observable.create(new Observable.OnSubscribe<List<Note>>() {
            @Override
            public void call(Subscriber<? super List<Note>> subscriber) {
                try {
                    subscriber.onNext(NoteTableHelper.getNotes(databaseHelper.getDatabase(), book, chapter));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    public Observable<List<Note>> loadNotes() {
        return Observable.create(new Observable.OnSubscribe<List<Note>>() {
            @Override
            public void call(Subscriber<? super List<Note>> subscriber) {
                try {
                    subscriber.onNext(NoteTableHelper.getNotes(databaseHelper.getDatabase()));
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }
}
