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

package net.zionsoft.obadiah.biblereading;

import net.zionsoft.obadiah.AppComponent;
import net.zionsoft.obadiah.injection.components.ComponentFragment;

public class BibleReadingComponentFragment extends ComponentFragment<BibleReadingComponent> {
    static final String FRAGMENT_TAG = "net.zionsoft.obadiah.BibleReadingComponentFragment.FRAGMENT_TAG";

    static BibleReadingComponentFragment newInstance() {
        return new BibleReadingComponentFragment();
    }

    @Override
    protected BibleReadingComponent createComponent(AppComponent appComponent) {
        return BibleReadingComponent.Initializer.init(appComponent);
    }
}
