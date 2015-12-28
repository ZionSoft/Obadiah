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

package net.zionsoft.obadiah.biblereading;

import net.zionsoft.obadiah.injection.scopes.ActivityScope;
import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.ReadingProgressModel;

import dagger.Module;
import dagger.Provides;

@Module
public class BibleReadingModule {
    @Provides
    @ActivityScope
    BibleReadingPresenter provideBibleReadingPresenter(BibleReadingModel bibleReadingModel,
                                                       ReadingProgressModel readingProgressModel) {
        return new BibleReadingPresenter(bibleReadingModel, readingProgressModel);
    }

    @Provides
    @ActivityScope
    VersePresenter provideVersePresenter(BibleReadingModel bibleReadingModel) {
        return new VersePresenter(bibleReadingModel);
    }
}
