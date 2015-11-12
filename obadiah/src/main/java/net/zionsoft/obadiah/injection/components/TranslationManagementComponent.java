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

package net.zionsoft.obadiah.injection.components;

import net.zionsoft.obadiah.injection.InjectionComponent;
import net.zionsoft.obadiah.injection.modules.TranslationManagementModule;
import net.zionsoft.obadiah.injection.scopes.ActivityScope;
import net.zionsoft.obadiah.ui.activities.TranslationManagementActivity;

import dagger.Subcomponent;

@ActivityScope
@Subcomponent(modules = TranslationManagementModule.class)
public interface TranslationManagementComponent extends Component {
    void inject(TranslationManagementActivity translationManagementActivity);

    final class Initializer {
        public static TranslationManagementComponent init(InjectionComponent injectionComponent) {
            return injectionComponent.plus(new TranslationManagementModule());
        }
    }
}
