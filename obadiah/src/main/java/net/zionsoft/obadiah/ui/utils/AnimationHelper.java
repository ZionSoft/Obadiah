/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2014 ZionSoft
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

import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import net.zionsoft.obadiah.R;

public class AnimationHelper {
    public static void slideIn(Activity activity, Intent startIntent) {
        activity.startActivity(startIntent);
        activity.overridePendingTransition(R.anim.slide_in_right_to_left, R.anim.fade_out);
    }

    public static void fadeIn(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            fadeInHoneyComb(view);
        else
            fadeInPreHoneyComb(view);
    }

    public static void fadeOut(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            fadeOutHoneyComb(view);
        else
            fadeOutPreHoneyComb(view);
    }

    private static final long ANIMATION_DURATION = 300L;

    // implementation for pre HONEYCOMB

    private static class SimpleAnimationListener implements Animation.AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }

    private static void fadeInPreHoneyComb(final View view) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0F, 1.0F);
        alphaAnimation.setDuration(ANIMATION_DURATION);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(alphaAnimation);
    }

    private static void fadeOutPreHoneyComb(final View view) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0F, 0.0F);
        alphaAnimation.setDuration(ANIMATION_DURATION);
        alphaAnimation.setAnimationListener(new SimpleAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }
        });
        view.setVisibility(View.VISIBLE);
        view.startAnimation(alphaAnimation);
    }

    // implementation for HONEYCOMB and above

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void fadeInHoneyComb(final View view) {
        view.setAlpha(0.0F);
        view.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(view, "alpha", 0.0F, 1.0F).setDuration(ANIMATION_DURATION).start();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void fadeOutHoneyComb(final View view) {
        view.setAlpha(1.0F);
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 1.0F, 0.0F).setDuration(ANIMATION_DURATION);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                view.setVisibility(View.GONE);
                view.setAlpha(1.0F);
            }
        });
        animator.start();
    }
}
