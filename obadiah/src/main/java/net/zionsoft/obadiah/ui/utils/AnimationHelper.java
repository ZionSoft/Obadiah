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

package net.zionsoft.obadiah.ui.utils;

import android.animation.Animator;
import android.view.View;

public class AnimationHelper {
    private static final long ANIMATION_DURATION = 300L;

    public static void fadeIn(View view) {
        view.setAlpha(0.0F);
        view.setVisibility(View.VISIBLE);
        view.animate().alpha(1.0F).setDuration(ANIMATION_DURATION).start();
    }

    public static void fadeOut(final View view) {
        view.setAlpha(1.0F);
        view.animate().alpha(0.0F).setDuration(ANIMATION_DURATION)
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        // do nothing
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                        view.setAlpha(1.0F);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        // do nothing
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                        // do nothing
                    }
                }).start();
    }
}
