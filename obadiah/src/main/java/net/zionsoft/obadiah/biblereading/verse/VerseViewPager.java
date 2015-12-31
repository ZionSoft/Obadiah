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

package net.zionsoft.obadiah.biblereading.verse;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Verse;

import java.util.List;

import javax.inject.Inject;

public class VerseViewPager extends ViewPager implements VersePagerView {
    @Inject
    Settings settings;

    @Inject
    VersePagerPresenter versePagerPresenter;

    @Inject
    VersePresenter versePresenter;

    private VersePagerAdapter adapter;

    private int currentChapter;

    public VerseViewPager(Context context) {
        super(context);
        initialize(context);
    }

    public VerseViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    private void initialize(Context context) {
        BibleReadingVerseComponent.Initializer.init(App.getInjectionComponent(context)).inject(this);

        adapter = new VersePagerAdapter(context, settings, versePresenter, getOffscreenPageLimit());
        setAdapter(adapter);
        addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (currentChapter == position) {
                    return;
                }
                versePresenter.saveReadingProgress(versePresenter.loadCurrentBook(), position, 0);
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        versePagerPresenter.takeView(this);
        currentChapter = versePresenter.loadCurrentChapter();
        setCurrentItem(currentChapter);

        versePresenter.takeView(adapter);
    }

    @Override
    protected void onDetachedFromWindow() {
        versePagerPresenter.dropView();
        versePresenter.dropView();
        super.onDetachedFromWindow();
    }

    @Override
    public void onReadingProgressUpdated(Verse.Index index) {
        if (currentChapter == index.chapter) {
            return;
        }
        currentChapter = index.chapter;

        setCurrentItem(currentChapter, true);
    }

    public void setVerseSelectionListener(VerseSelectionListener listener) {
        adapter.setVerseSelectionListener(listener);
    }

    @Nullable
    public List<Verse> getSelectedVerses() {
        return adapter.getSelectedVerses(getCurrentItem());
    }

    public void deselectVerses() {
        adapter.deselectVerses();
    }
}
