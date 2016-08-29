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

package net.zionsoft.obadiah.biblereading.verse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;

import java.util.List;

class VerseItemAnimator extends DefaultItemAnimator {
    static class VerseItemHolderInfo extends ItemHolderInfo {
        static final Integer ACTION_ADD_BOOKMARK = 1;
        static final Integer ACTION_REMOVE_BOOKMARK = 2;
        static final Integer ACTION_SHOW_NOTE = 3;
        static final Integer ACTION_HIDE_NOTE = 4;
        static final Integer ACTION_UPDATE_NOTE = 5;
        static final Integer ACTION_REMOVE_NOTE = 6;

        final Integer action;

        VerseItemHolderInfo(Integer action) {
            this.action = action;
        }
    }

    final DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
    final OvershootInterpolator overshootInterpolator = new OvershootInterpolator(4);

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
        return true;
    }

    @NonNull
    @Override
    public RecyclerView.ItemAnimator.ItemHolderInfo recordPreLayoutInformation(
            @NonNull RecyclerView.State state, @NonNull RecyclerView.ViewHolder viewHolder,
            int changeFlags, @NonNull List<Object> payloads) {
        if (changeFlags == FLAG_CHANGED) {
            final int payloadCount = payloads.size();
            for (int i = 0; i < payloadCount; ++i) {
                final Object payload = payloads.get(i);
                if (payload instanceof Integer) {
                    return new VerseItemHolderInfo((Integer) payload);
                }
            }
        }
        return super.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads);
    }

    @Override
    public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder,
                                 @NonNull RecyclerView.ViewHolder newHolder,
                                 @NonNull RecyclerView.ItemAnimator.ItemHolderInfo preLayoutInfo,
                                 @NonNull RecyclerView.ItemAnimator.ItemHolderInfo postLayoutInfo) {
        if (oldHolder instanceof VerseItemViewHolder && preLayoutInfo instanceof VerseItemHolderInfo) {
            final VerseItemViewHolder holder = (VerseItemViewHolder) oldHolder;
            final Integer action = ((VerseItemHolderInfo) preLayoutInfo).action;
            if (VerseItemHolderInfo.ACTION_ADD_BOOKMARK.equals(action)) {
                holder.bookmarkIcon.animate().scaleX(1.5F).scaleY(1.5F)
                        .setDuration(250L)
                        .setInterpolator(decelerateInterpolator)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                holder.setBookmark(true);
                                holder.bookmarkIcon.animate().scaleX(1.0F).scaleY(1.0F)
                                        .setInterpolator(overshootInterpolator)
                                        .setDuration(500L)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                dispatchAnimationFinished(holder);
                                            }
                                        });
                            }
                        });
            } else if (VerseItemHolderInfo.ACTION_REMOVE_BOOKMARK.equals(action)) {
                holder.setBookmark(false);
                dispatchAnimationFinished(holder);
            } else if (VerseItemHolderInfo.ACTION_SHOW_NOTE.equals(action)) {
                AnimationHelper.fadeIn(holder.note);
                holder.noteIcon.animate().rotationX(90.0F)
                        .setDuration(150L)
                        .setInterpolator(decelerateInterpolator)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                holder.noteIcon.setImageResource(R.drawable.ic_arrow_up);
                                holder.noteIcon.animate().rotationX(0.0F)
                                        .setInterpolator(decelerateInterpolator)
                                        .setDuration(150L)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                dispatchAnimationFinished(holder);
                                            }
                                        });
                            }
                        });
            } else if (VerseItemHolderInfo.ACTION_HIDE_NOTE.equals(action)) {
                holder.noteIcon.animate().rotationX(90.0F)
                        .setDuration(150L)
                        .setInterpolator(decelerateInterpolator)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                holder.noteIcon.setImageResource(R.drawable.ic_note);
                                holder.noteIcon.animate().rotationX(0.0F)
                                        .setInterpolator(decelerateInterpolator)
                                        .setDuration(150L)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                dispatchAnimationFinished(holder);
                                            }
                                        });
                            }
                        });
            } else if (VerseItemHolderInfo.ACTION_UPDATE_NOTE.equals(action)) {
                holder.noteIcon.setColorFilter(VerseItemViewHolder.ON);
                dispatchAnimationFinished(holder);
            } else if (VerseItemHolderInfo.ACTION_REMOVE_NOTE.equals(action)) {
                holder.noteIcon.setColorFilter(VerseItemViewHolder.OFF);
                dispatchAnimationFinished(holder);
            }
            return false;
        }
        return super.animateChange(oldHolder, newHolder, preLayoutInfo, postLayoutInfo);
    }
}
