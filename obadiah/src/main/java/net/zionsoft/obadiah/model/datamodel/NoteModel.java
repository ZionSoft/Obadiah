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

import net.zionsoft.obadiah.model.database.DatabaseHelper;
import net.zionsoft.obadiah.model.database.NoteTableHelper;
import net.zionsoft.obadiah.model.domain.Note;
import net.zionsoft.obadiah.model.domain.VerseIndex;

import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.Single;
import rx.functions.Func0;

@Singleton
public class NoteModel {
    final DatabaseHelper databaseHelper;

    @Inject
    public NoteModel(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public Single<Note> updateNote(final VerseIndex verseIndex, final String note) {
        return Single.fromCallable(new Callable<Note>() {
            @Override
            public Note call() throws Exception {
                final Note n = Note.create(verseIndex, note, System.currentTimeMillis());
                NoteTableHelper.saveNote(databaseHelper.getDatabase(), n);
                return n;
            }
        });
    }

    public Observable<Void> removeNote(final VerseIndex verseIndex) {
        return Observable.defer(new Func0<Observable<Void>>() {
            @Override
            public Observable<Void> call() {
                try {
                    NoteTableHelper.removeNote(databaseHelper.getDatabase(), verseIndex);
                    return Observable.empty();
                } catch (Exception e) {
                    return Observable.error(e);
                }
            }
        });
    }

    public Observable<List<Note>> loadNotes(final int book, final int chapter) {
        return Observable.fromCallable(new Callable<List<Note>>() {
            @Override
            public List<Note> call() throws Exception {
                return NoteTableHelper.getNotes(databaseHelper.getDatabase(), book, chapter);
            }
        });
    }

    public Single<List<Note>> loadNotes() {
        return Single.fromCallable(new Callable<List<Note>>() {
            @Override
            public List<Note> call() throws Exception {
                return NoteTableHelper.getNotes(databaseHelper.getDatabase());
            }
        });
    }
}
