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

package net.zionsoft.obadiah.notes;

import android.support.v4.util.Pair;

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.NoteModel;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Note;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.mvp.BasePresenter;
import net.zionsoft.obadiah.utils.RxHelper;

import java.util.ArrayList;
import java.util.List;

import rx.SingleSubscriber;
import rx.Subscription;
import rx.functions.Func1;

class NotesPresenter extends BasePresenter<NotesView> {
    @SuppressWarnings("WeakerAccess")
    final BibleReadingModel bibleReadingModel;
    private final NoteModel noteModel;

    @SuppressWarnings("WeakerAccess")
    Subscription subscription;

    NotesPresenter(BibleReadingModel bibleReadingModel, NoteModel noteModel, Settings settings) {
        super(settings);
        this.bibleReadingModel = bibleReadingModel;
        this.noteModel = noteModel;
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }

    void loadNotes() {
        subscription = noteModel.loadNotes()
                .map(new Func1<List<Note>, Pair<List<Note>, List<Verse>>>() {
                    @Override
                    public Pair<List<Note>, List<Verse>> call(List<Note> notes) {
                        final int count = notes.size();
                        final List<Verse> verses = new ArrayList<>(count);
                        final String translation = bibleReadingModel.loadCurrentTranslation();
                        for (int i = 0; i < count; ++i) {
                            final VerseIndex verseIndex = notes.get(i).verseIndex();
                            verses.add(bibleReadingModel.loadVerse(translation, verseIndex.book(),
                                    verseIndex.chapter(), verseIndex.verse()).toBlocking().value());
                        }
                        return new Pair<>(notes, verses);
                    }
                }).compose(RxHelper.<Pair<List<Note>, List<Verse>>>applySchedulersForSingle())
                .subscribe(new SingleSubscriber<Pair<List<Note>, List<Verse>>>() {
                    @Override
                    public void onSuccess(Pair<List<Note>, List<Verse>> notes) {
                        subscription = null;
                        final NotesView v = getView();
                        if (v != null) {
                            v.onNotesLoaded(notes.first, notes.second);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        subscription = null;
                        final NotesView v = getView();
                        if (v != null) {
                            v.onNotesLoadFailed();
                        }
                    }
                });
    }

    void saveReadingProgress(VerseIndex index) {
        bibleReadingModel.saveReadingProgress(index);
    }
}
