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

package net.zionsoft.obadiah.mvp.presenters;

import android.text.TextUtils;

import net.zionsoft.obadiah.model.translations.TranslationInfo;
import net.zionsoft.obadiah.model.translations.Translations;
import net.zionsoft.obadiah.mvp.models.BibleReadingModel;
import net.zionsoft.obadiah.mvp.models.TranslationManagementModel;
import net.zionsoft.obadiah.mvp.views.TranslationManagementView;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class TranslationManagementPresenter extends MVPPresenter<TranslationManagementView> {
    private final BibleReadingModel bibleReadingModel;
    private final TranslationManagementModel translationManagementModel;

    private Subscription loadTranslationsSubscription;
    private Subscription removeTranslationSubscription;
    private Subscription fetchTranslationSubscription;

    public TranslationManagementPresenter(BibleReadingModel bibleReadingModel,
                                          TranslationManagementModel translationManagementModel) {
        this.bibleReadingModel = bibleReadingModel;
        this.translationManagementModel = translationManagementModel;
    }

    @Override
    protected void onViewDropped() {
        if (loadTranslationsSubscription != null) {
            loadTranslationsSubscription.unsubscribe();
            loadTranslationsSubscription = null;
        }

        super.onViewDropped();
    }

    public String loadCurrentTranslation() {
        return bibleReadingModel.loadCurrentTranslation();
    }

    public void saveCurrentTranslation(TranslationInfo translation) {
        bibleReadingModel.saveCurrentTranslation(translation);
    }

    public void loadTranslations(boolean forceRefresh) {
        loadTranslationsSubscription = translationManagementModel.loadTranslations(forceRefresh)
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        loadTranslationsSubscription = null;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Translations>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationLoadFailed();
                        }
                    }

                    @Override
                    public void onNext(Translations translations) {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationLoaded(translations);
                        }
                    }
                });
    }

    public void removeTranslation(final TranslationInfo translation) {
        if (removeTranslationSubscription != null) {
            return;
        }

        removeTranslationSubscription = translationManagementModel.removeTranslation(translation)
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        removeTranslationSubscription = null;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationRemoved(translation);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationRemovalFailed(translation);
                        }
                    }

                    @Override
                    public void onNext(Void aVoid) {
                        // won't reach here
                    }
                });
    }

    public void cancelRemoveTranslation() {
        if (removeTranslationSubscription != null) {
            removeTranslationSubscription.unsubscribe();
            removeTranslationSubscription = null;
        }
    }

    public void fetchTranslation(final TranslationInfo translation) {
        if (fetchTranslationSubscription != null) {
            return;
        }

        fetchTranslationSubscription = translationManagementModel.fetchTranslation(translation)
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        fetchTranslationSubscription = null;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                        if (TextUtils.isEmpty(bibleReadingModel.loadCurrentTranslation())) {
                            bibleReadingModel.saveCurrentTranslation(translation);
                        }

                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationDownloaded(translation);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationDownloadFailed(translation);
                        }
                    }

                    @Override
                    public void onNext(Integer progress) {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationDownloadProgressed(translation, progress);
                        }
                    }
                });
    }

    public void cancelFetchTranslation() {
        if (fetchTranslationSubscription != null) {
            fetchTranslationSubscription.unsubscribe();
            fetchTranslationSubscription = null;
        }
    }
}
