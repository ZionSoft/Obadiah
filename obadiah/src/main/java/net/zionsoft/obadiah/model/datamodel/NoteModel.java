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
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Pair;

import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.database.NoteTableHelper;
import net.zionsoft.obadiah.model.domain.Note;
import net.zionsoft.obadiah.model.domain.VerseIndex;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Completable;
import rx.CompletableSubscriber;
import rx.Observable;
import rx.Single;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

@Singleton
public class NoteModel {
    public static final int ACTION_ADD = 0;
    public static final int ACTION_REMOVE = 1;

    @IntDef({ACTION_ADD, ACTION_REMOVE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Action {
    }

    @SuppressWarnings("WeakerAccess")
    final SerializedSubject<Pair<Integer, Note>, Pair<Integer, Note>> notesUpdatesSubject
            = PublishSubject.<Pair<Integer, Note>>create().toSerialized();

    @SuppressWarnings("WeakerAccess")
    final DatabaseHelper databaseHelper;

    @Inject
    public NoteModel(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    @NonNull
    public Observable<Pair<Integer, Note>> observeNotes() {
        return notesUpdatesSubject.asObservable();
    }

    @NonNull
    public Single<Note> updateNote(final VerseIndex verseIndex, final String note) {
        return updateNote(verseIndex, note, System.currentTimeMillis());
    }

    @NonNull
    public Single<Note> updateNote(final VerseIndex verseIndex, final String note, final long timestamp) {
        return Single.fromCallable(new Callable<Note>() {
            @Override
            public Note call() throws Exception {
                final SQLiteDatabase db = databaseHelper.getDatabase();
                try {
                    db.beginTransaction();

                    Note n = NoteTableHelper.getNote(db, verseIndex);
                    if (n == null || timestamp > n.timestamp()) {
                        n = Note.create(verseIndex, note, timestamp);
                        NoteTableHelper.saveNote(databaseHelper.getDatabase(), n);
                        notesUpdatesSubject.onNext(new Pair<>(ACTION_ADD, n));
                    }

                    db.setTransactionSuccessful();
                    return n;
                } finally {
                    if (db.inTransaction()) {
                        db.endTransaction();
                    }
                }
            }
        });
    }

    @NonNull
    public Completable removeNote(final VerseIndex verseIndex) {
        return Completable.create(new Completable.OnSubscribe() {
            @Override
            public void call(CompletableSubscriber subscriber) {
                final SQLiteDatabase db = databaseHelper.getDatabase();
                try {
                    db.beginTransaction();

                    final Note note = NoteTableHelper.getNote(db, verseIndex);
                    if (note != null) {
                        NoteTableHelper.removeNote(databaseHelper.getDatabase(), verseIndex);
                        notesUpdatesSubject.onNext(new Pair<>(ACTION_REMOVE, note));
                    }

                    db.setTransactionSuccessful();
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                } finally {
                    if (db.inTransaction()) {
                        db.endTransaction();
                    }
                }
            }
        });
    }

    @NonNull
    public Observable<List<Note>> loadNotes(final int book, final int chapter) {
        return Observable.fromCallable(new Callable<List<Note>>() {
            @Override
            public List<Note> call() throws Exception {
                return NoteTableHelper.getNotes(databaseHelper.getDatabase(), book, chapter);
            }
        });
    }

    @NonNull
    public Single<List<Note>> loadNotes() {
        return Single.fromCallable(new Callable<List<Note>>() {
            @Override
            public List<Note> call() throws Exception {
                return NoteTableHelper.getNotes(databaseHelper.getDatabase());
            }
        });
    }
}
