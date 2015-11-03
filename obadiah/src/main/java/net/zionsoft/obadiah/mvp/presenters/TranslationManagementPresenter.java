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

import net.zionsoft.obadiah.model.translations.TranslationInfo;
import net.zionsoft.obadiah.model.translations.Translations;
import net.zionsoft.obadiah.mvp.models.TranslationManagementModel;
import net.zionsoft.obadiah.mvp.views.TranslationManagementView;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class TranslationManagementPresenter extends MVPPresenter<TranslationManagementView> {
    private final TranslationManagementModel translationManagementModel;

    private CompositeSubscription subscription;

    public TranslationManagementPresenter(TranslationManagementModel translationManagementModel) {
        this.translationManagementModel = translationManagementModel;
    }

    @Override
    protected void onViewTaken() {
        super.onViewTaken();
        subscription = new CompositeSubscription();
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }

    public void loadTranslations(boolean forceRefresh) {
        subscription.add(translationManagementModel.loadTranslations(forceRefresh)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Translations>() {
                    @Override
                    public void call(Translations translations) {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            if (translations != null) {
                                v.onTranslationLoaded(translations);
                            } else {
                                v.onTranslationLoadFailed();
                            }
                        }
                    }
                }));
    }

    public void removeTranslation(final TranslationInfo translation) {
        subscription.add(translationManagementModel.removeTranslation(translation)
                .subscribeOn(Schedulers.io())
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
                }));
    }

    public void fetchTranslation(final TranslationInfo translation) {
        subscription.add(translationManagementModel.fetchTranslation(translation)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
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
                }));
    }
}
