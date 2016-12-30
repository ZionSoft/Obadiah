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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.zionsoft.obadiah.model.domain.Bible;
import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.Note;
import net.zionsoft.obadiah.model.domain.ReadingProgress;
import net.zionsoft.obadiah.model.domain.User;
import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.utils.Triple;
import net.zionsoft.obadiah.utils.UriBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Completable;
import rx.Observable;
import rx.Observer;
import rx.Single;
import rx.Subscription;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

@Singleton
public class SyncModel implements ChildEventListener {
    private static final StringBuilder STRING_BUILDER = new StringBuilder();

    private final UserModel userModel;
    private final BibleReadingModel bibleReadingModel;
    private final BookmarkModel bookmarkModel;
    private final NoteModel noteModel;
    private final ReadingProgressModel readingProgressModel;

    @SuppressWarnings("WeakerAccess")
    final FirebaseDatabase firebaseDatabase;

    @SuppressWarnings("WeakerAccess")
    DatabaseReference bookmarksReference;
    @SuppressWarnings("WeakerAccess")
    DatabaseReference notesReference;
    @SuppressWarnings("WeakerAccess")
    DatabaseReference readingProgressReference;
    @SuppressWarnings("WeakerAccess")
    DatabaseReference metadataReference;

    private Subscription observeBookmarksSubscription;
    private Subscription observeNotesSubscription;
    private Subscription observeReadingProgressSubscription;

    @Inject
    public SyncModel(UserModel userModel, BibleReadingModel bibleReadingModel,
                     BookmarkModel bookmarkModel, NoteModel noteModel,
                     final ReadingProgressModel readingProgressModel) {
        this.userModel = userModel;
        this.bibleReadingModel = bibleReadingModel;
        this.bookmarkModel = bookmarkModel;
        this.noteModel = noteModel;
        this.readingProgressModel = readingProgressModel;

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.setPersistenceEnabled(true);

        userModel.observeCurrentUser().observeOn(Schedulers.io())
                .subscribe(new Observer<User>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(User user) {
                        unsubscribeAll();
                        if (user != null) {
                            initialSync(user);
                        } else {
                            bookmarksReference = null;
                            notesReference = null;
                            readingProgressReference = null;
                            metadataReference = null;
                        }
                    }
                });
    }

    @SuppressWarnings("WeakerAccess")
    void unsubscribeAll() {
        unsubscribeObserveBookmarks();
        unsubscribeObserveNotes();
        unsubscribeObserveReadingProgress();
    }

    private void unsubscribeObserveBookmarks() {
        if (observeBookmarksSubscription != null) {
            observeBookmarksSubscription.unsubscribe();
            observeBookmarksSubscription = null;
        }
    }

    private void unsubscribeObserveNotes() {
        if (observeNotesSubscription != null) {
            observeNotesSubscription.unsubscribe();
            observeNotesSubscription = null;
        }
    }

    private void unsubscribeObserveReadingProgress() {
        if (observeReadingProgressSubscription != null) {
            observeReadingProgressSubscription.unsubscribe();
            observeReadingProgressSubscription = null;
        }
    }

    @SuppressWarnings("WeakerAccess")
    void initialSync(@NonNull User user) {
        syncBookmarks(user);
        syncNotes(user);
        syncReadingProgress(user);
    }

    private void syncBookmarks(@NonNull final User user) {
        bookmarksReference = firebaseDatabase.getReference(buildBookmarksRootPath(user));
        bookmarksReference.addChildEventListener(this);

        bookmarkModel.loadBookmarks().map(new Func1<List<Bookmark>, Void>() {
            @Override
            public Void call(List<Bookmark> bookmarks) {
                final int count = bookmarks.size();
                final Map<String, Object> updates = new HashMap<>(count);
                for (int i = 0; i < count; ++i) {
                    final Bookmark bookmark = bookmarks.get(i);
                    updates.put(verseIndexToKey(bookmark.verseIndex()), bookmark.timestamp());
                }
                bookmarksReference.updateChildren(updates);

                return null;
            }
        }).onErrorResumeNext(Single.<Void>just(null)).subscribeOn(Schedulers.io()).subscribe();

        unsubscribeObserveBookmarks();
        observeBookmarksSubscription = bookmarkModel.observeBookmarks().observeOn(Schedulers.io())
                .subscribe(new Observer<Pair<Integer, Bookmark>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(Pair<Integer, Bookmark> bookmark) {
                        if (bookmarksReference != null) {
                            final DatabaseReference reference = bookmarksReference.child(
                                    verseIndexToKey(bookmark.second.verseIndex()));
                            switch (bookmark.first) {
                                case BookmarkModel.ACTION_ADD:
                                    final long timestamp = bookmark.second.timestamp();
                                    reference.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot snapshot) {
                                            if (!snapshot.exists()) {
                                                reference.setValue(timestamp);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            // do nothing
                                        }
                                    });
                                    break;
                                case BookmarkModel.ACTION_REMOVE:
                                    reference.removeValue();
                                    break;
                            }
                        }
                    }
                });
    }

    private static String buildBookmarksRootPath(@NonNull User user) {
        synchronized (STRING_BUILDER) {
            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append("/bookmarks/").append(user.uid);
            return STRING_BUILDER.toString();
        }
    }

    @SuppressWarnings("WeakerAccess")
    static String verseIndexToKey(@NonNull VerseIndex verseIndex) {
        synchronized (STRING_BUILDER) {
            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(verseIndex.book()).append(':')
                    .append(verseIndex.chapter()).append(':').append(verseIndex.verse());
            return STRING_BUILDER.toString();
        }
    }

    private void syncNotes(@NonNull final User user) {
        notesReference = firebaseDatabase.getReference(buildNotesRootPath(user));
        notesReference.addChildEventListener(this);

        noteModel.loadNotes().map(new Func1<List<Note>, Void>() {
            @Override
            public Void call(List<Note> notes) {
                for (int i = notes.size() - 1; i >= 0; --i) {
                    updateNote(notes.get(i));
                }

                return null;
            }
        }).onErrorResumeNext(Single.<Void>just(null)).subscribeOn(Schedulers.io()).subscribe();

        unsubscribeObserveNotes();
        observeNotesSubscription = noteModel.observeNotes().observeOn(Schedulers.io())
                .subscribe(new Observer<Pair<Integer, Note>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(Pair<Integer, Note> note) {
                        if (notesReference != null) {
                            switch (note.first) {
                                case NoteModel.ACTION_UPDATED:
                                    updateNote(note.second);
                                    break;
                                case NoteModel.ACTION_REMOVE:
                                    notesReference.child(verseIndexToKey(note.second.verseIndex()))
                                            .removeValue();
                                    break;
                            }
                        }
                    }
                });
    }

    private static String buildNotesRootPath(@NonNull User user) {
        synchronized (STRING_BUILDER) {
            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append("/notes/").append(user.uid);
            return STRING_BUILDER.toString();
        }
    }

    @SuppressWarnings("WeakerAccess")
    void updateNote(@NonNull final Note note) {
        if (notesReference == null) {
            return;
        }

        final DatabaseReference reference = notesReference.child(verseIndexToKey(note.verseIndex()));
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    reference.child("timestamp").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (!dataSnapshot.exists() || note.timestamp() > (long) dataSnapshot.getValue()) {
                                updateNote(note, reference);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            // do nothing
                        }
                    });
                } else {
                    updateNote(note, reference);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // do nothing
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    void updateNote(@NonNull Note note, @NonNull DatabaseReference reference) {
        final Map<String, Object> updates = new HashMap<>(2);
        updates.put("timestamp", note.timestamp());
        updates.put("note", note.note());
        reference.updateChildren(updates);

        final Indexable indexable = Indexables.noteDigitalDocumentBuilder()
                .setName("")
                .setUrl(UriBuilder.createUri(bibleReadingModel.loadCurrentTranslation(),
                        note.verseIndex().book(), note.verseIndex().chapter(), note.verseIndex().verse()))
                .setDateModified(new Date(note.timestamp()))
                .setText(note.note())
                .setMetadata(new Indexable.Metadata.Builder().setWorksOffline(true))
                .build();
        FirebaseAppIndex.getInstance().update(indexable);
    }

    private void syncReadingProgress(@NonNull final User user) {
        readingProgressReference = firebaseDatabase.getReference(buildReadingProgressRootPath(user));
        readingProgressReference.addChildEventListener(this);
        metadataReference = firebaseDatabase.getReference(buildMetadataRootPath(user));
        metadataReference.addChildEventListener(this);

        readingProgressModel.loadReadingProgress().map(new Func1<ReadingProgress, Void>() {
            @Override
            public Void call(ReadingProgress readingProgress) {
                final int booksCount = Bible.getBookCount();
                final Map<String, Object> readingProgressUpdates = new HashMap<>();
                for (int i = 0; i < booksCount; ++i) {
                    final List<ReadingProgress.ReadChapter> readChapters = readingProgress.getReadChapters(i);
                    for (int j = readChapters.size() - 1; j >= 0; --j) {
                        final ReadingProgress.ReadChapter readChapter = readChapters.get(j);
                        readingProgressUpdates.put(verseIndexToKey(i, readChapter.chapter), readChapter.timestamp);
                    }
                }
                readingProgressReference.updateChildren(readingProgressUpdates);

                syncMetadata(metadataReference, readingProgress.getContinuousReadingDays(),
                        readingProgress.getLastReadingTimestamp());

                return null;
            }
        }).onErrorResumeNext(Observable.<Void>just(null)).subscribeOn(Schedulers.io()).subscribe();

        unsubscribeObserveReadingProgress();
        observeReadingProgressSubscription = readingProgressModel.observeReadingProgress().observeOn(Schedulers.io())
                .subscribe(new Observer<Triple<ReadingProgress.ReadChapter, Integer, Long>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(Triple<ReadingProgress.ReadChapter, Integer, Long> readChapter) {
                        if (readingProgressReference == null || metadataReference == null) {
                            return;
                        }
                        readingProgressReference.child(verseIndexToKey(readChapter.first.book, readChapter.first.chapter))
                                .setValue(readChapter.first.timestamp);
                        syncMetadata(metadataReference, readChapter.second, readChapter.third);
                    }
                });
    }

    @SuppressWarnings("WeakerAccess")
    static void syncMetadata(@NonNull DatabaseReference metadataReference, int continuousReadingDays,
                             long lastReadingTimestamp) {
        final Map<String, Object> metadataUpdates = new HashMap<>(2);
        metadataUpdates.put("continuousReadingDays", continuousReadingDays);
        metadataUpdates.put("lastReadingTimestamp", lastReadingTimestamp);
        metadataReference.updateChildren(metadataUpdates);
    }

    private static String buildReadingProgressRootPath(@NonNull User user) {
        synchronized (STRING_BUILDER) {
            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append("/readingProgress/").append(user.uid);
            return STRING_BUILDER.toString();
        }
    }

    private static String buildMetadataRootPath(@NonNull User user) {
        synchronized (STRING_BUILDER) {
            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append("/metadata/").append(user.uid);
            return STRING_BUILDER.toString();
        }
    }

    @SuppressWarnings("WeakerAccess")
    static String verseIndexToKey(int book, int chapter) {
        synchronized (STRING_BUILDER) {
            STRING_BUILDER.setLength(0);
            STRING_BUILDER.append(book).append(':').append(chapter);
            return STRING_BUILDER.toString();
        }
    }

    @Override
    public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
        final String type = getSnapshotType(snapshot);
        if (TextUtils.isEmpty(type)) {
            return;
        }

        if ("readingProgress".equals(type)) {
            final String[] fields = snapshot.getKey().split(":");
            if (fields.length != 2) {
                return;
            }
            final int book = Integer.parseInt(fields[0]);
            final int chapter = Integer.parseInt(fields[1]);
            final Long timestamp = (Long) snapshot.getValue();
            if (timestamp != null) {
                readingProgressModel.trackReadingProgress(book, chapter, timestamp)
                        .subscribeOn(Schedulers.io()).onErrorComplete().subscribe();
            }
        } else if ("metadata".equals(type)) {
            final String key = snapshot.getKey();
            Completable completable = null;
            if ("continuousReadingDays".equals(key)) {
                final Long continuousReadingDays = (Long) snapshot.getValue();
                if (continuousReadingDays != null) {
                    completable = readingProgressModel
                            .updateContinuousReadingDays(continuousReadingDays.intValue());
                }
            } else if ("lastReadingTimestamp".equals(key)) {
                final Long lastReaingTimestamp = (Long) snapshot.getValue();
                if (lastReaingTimestamp != null) {
                    completable = readingProgressModel.updateLastReadingTimestamp(lastReaingTimestamp);
                }
            }
            if (completable != null) {
                completable.subscribeOn(Schedulers.io()).onErrorComplete().subscribe();
            }
        } else {
            final VerseIndex verseIndex = keyToVerseIndex(snapshot.getKey());
            if (verseIndex != null) {
                if ("bookmarks".equals(type)) {
                    final Long timestamp = (Long) snapshot.getValue();
                    if (timestamp != null) {
                        bookmarkModel.addBookmark(verseIndex, timestamp).subscribeOn(Schedulers.io())
                                .onErrorResumeNext(Single.<Bookmark>just(null)).subscribe();
                    }
                } else if ("notes".equals(type)) {
                    final String note = (String) snapshot.child("note").getValue();
                    final Long timestamp = (Long) snapshot.child("timestamp").getValue();
                    if (!TextUtils.isEmpty(note) && timestamp != null) {
                        noteModel.updateNote(verseIndex, note, timestamp).subscribeOn(Schedulers.io())
                                .onErrorResumeNext(Single.<Note>just(null)).subscribe();
                    }
                }
            }
        }
    }

    @Nullable
    private String getSnapshotType(DataSnapshot snapshot) {
        final User user = userModel.getCurrentUser();
        if (user == null) {
            return null;
        }
        DatabaseReference parent = snapshot.getRef().getParent();
        if (parent == null || !user.uid.equals(parent.getKey())) {
            return null;
        }
        parent = parent.getParent();
        return parent != null ? parent.getKey() : null;
    }

    @Nullable
    private static VerseIndex keyToVerseIndex(String key) {
        final String[] fields = key.split(":");
        if (fields.length != 3) {
            return null;
        }
        return VerseIndex.create(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]),
                Integer.parseInt(fields[2]));
    }

    @Override
    public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
        onChildAdded(snapshot, previousChildName);
    }

    @Override
    public void onChildRemoved(DataSnapshot snapshot) {
        final String type = getSnapshotType(snapshot);
        if (TextUtils.isEmpty(type)) {
            return;
        }

        final VerseIndex verseIndex = keyToVerseIndex(snapshot.getKey());
        if (verseIndex != null) {
            if ("bookmarks".equals(type)) {
                bookmarkModel.removeBookmark(verseIndex).subscribeOn(Schedulers.io())
                        .onErrorComplete().subscribe();
            } else if ("notes".equals(type)) {
                noteModel.removeNote(verseIndex).subscribeOn(Schedulers.io())
                        .onErrorComplete().subscribe();
            }
        }
    }

    @Override
    public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
        // do nothing
    }

    @Override
    public void onCancelled(DatabaseError error) {
        // do nothing
    }
}
