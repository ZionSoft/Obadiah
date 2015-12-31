package net.zionsoft.obadiah.biblereading.verse;

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.mvp.MVPPresenter;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

class VersePagerPresenter extends MVPPresenter<VersePagerView> {
    private final BibleReadingModel bibleReadingModel;
    private Subscription subscription;

    VersePagerPresenter(BibleReadingModel bibleReadingModel) {
        this.bibleReadingModel = bibleReadingModel;
    }

    @Override
    protected void onViewTaken() {
        super.onViewTaken();

        subscription = bibleReadingModel.observeCurrentReadingProgress()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Verse.Index>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(Verse.Index index) {
                        final VersePagerView v = getView();
                        if (v != null) {
                            v.onReadingProgressUpdated(index);
                        }
                    }
                });
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }
}
