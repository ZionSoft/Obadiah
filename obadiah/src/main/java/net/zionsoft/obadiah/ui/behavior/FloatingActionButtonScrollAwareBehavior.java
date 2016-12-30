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

package net.zionsoft.obadiah.ui.behavior;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewPropertyAnimator;

public class FloatingActionButtonScrollAwareBehavior extends FloatingActionButton.Behavior {
    private boolean isHiding = false;

    public FloatingActionButtonScrollAwareBehavior() {
        super();
    }

    public FloatingActionButtonScrollAwareBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child,
                                       View directTargetChild, View target, int nestedScrollAxes) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL || super.onStartNestedScroll(
                coordinatorLayout, child, directTargetChild, target, nestedScrollAxes);
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, FloatingActionButton child,
                                  View target, int dx, int dy, int[] consumed) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);

        if (dy > 0) {
            hide(child);
        } else if (dy < 0) {
            show(child);
        }
    }

    private void hide(final FloatingActionButton fab) {
        if (isHiding) {
            return;
        }
        isHiding = true;

        final ViewPropertyAnimator animator = fab.animate();
        animator.cancel();
        animator.scaleX(0.0F).scaleY(0.0F).alpha(0.0F).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                fab.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void show(FloatingActionButton fab) {
        if (!isHiding) {
            return;
        }
        isHiding = false;

        fab.setVisibility(View.VISIBLE);

        ViewPropertyAnimator animator = fab.animate();
        animator.cancel();
        animator.setListener(null);
        animator.scaleX(1.0F).scaleY(1.0F).alpha(1.0F);
    }
}
