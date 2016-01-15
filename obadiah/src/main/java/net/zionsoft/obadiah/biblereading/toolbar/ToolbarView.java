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

package net.zionsoft.obadiah.biblereading.toolbar;

import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.mvp.MVPView;

import java.util.List;

interface ToolbarView extends MVPView {
    void onTranslationsLoaded(List<String> translations);

    void onBookNamesLoaded(List<String> bookNames);

    void onReadingProgressUpdated(Verse.Index index);
}
