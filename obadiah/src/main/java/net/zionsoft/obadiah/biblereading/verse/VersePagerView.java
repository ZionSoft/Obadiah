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

package net.zionsoft.obadiah.biblereading.verse;

import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.Note;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.mvp.MVPView;

import java.util.List;

interface VersePagerView extends MVPView {
    void onVersesLoaded(List<Verse> verses, List<Bookmark> bookmarks, List<Note> notes);

    void onVersesLoadFailed(int book, int chapter);

    void onBookmarkAdded(Bookmark bookmark);

    void onBookmarkAddFailed(VerseIndex verseIndex);

    void onBookmarkRemoved(VerseIndex verseIndex);

    void onBookmarkRemoveFailed(VerseIndex verseIndex);

    void onNoteUpdated(Note note);

    void onNoteUpdateFailed(VerseIndex verseIndex, String note);

    void onNoteRemoved(VerseIndex verseIndex);

    void onNoteRemoveFailed(VerseIndex verseIndex);

    void showNote(VerseIndex verseIndex);

    void hideNote(VerseIndex verseIndex);

    void onTranslationUpdated();

    void onReadingProgressChanged(VerseIndex verseIndex);
}
