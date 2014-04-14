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

package net.zionsoft.obadiah.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import net.zionsoft.obadiah.R;

public class ProgressBar extends View {
    private static final int DEFAULT_MAX_PROGRESS = 100;
    private static final int DEFAULT_TEXT_PADDING = 0;
    private static final int DEFAULT_TEXT_SIZE = 20;

    // TODO introduces custom attributes for them
    private Paint mBackGroundPaint;
    private Paint mProgressPaint;
    private Paint mFullProgressPaint;
    private Paint mTextPaint;

    private int mProgress;
    private int mMaxProgress = DEFAULT_MAX_PROGRESS;
    private int mTextPadding = DEFAULT_TEXT_PADDING;
    private String mText;

    public ProgressBar(Context context) {
        super(context);

        init(context);
    }

    public ProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
        setAttrs(attrs);
    }

    public ProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
        setAttrs(attrs);
    }

    private void init(Context context) {
        final Resources resources = context.getResources();
        mBackGroundPaint = new Paint();
        mBackGroundPaint.setColor(Color.LTGRAY);
        mProgressPaint = new Paint();
        mProgressPaint.setColor(resources.getColor(R.color.dark_cyan));
        mFullProgressPaint = new Paint();
        mFullProgressPaint.setColor(resources.getColor(R.color.dark_lime));
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextAlign(Paint.Align.RIGHT);
        mTextPaint.setTextSize(DEFAULT_TEXT_SIZE);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private void setAttrs(AttributeSet attrs) {
        if (attrs == null)
            return;

        final TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.ProgressBar, 0, 0);
        mMaxProgress = attributes.getInt(R.styleable.ProgressBar_maxProgress, DEFAULT_MAX_PROGRESS);
        mTextPadding = attributes.getDimensionPixelSize(R.styleable.ProgressBar_textPadding, DEFAULT_TEXT_PADDING);
        mTextPaint.setTextSize(attributes.getDimensionPixelSize(R.styleable.ProgressBar_textSize, DEFAULT_TEXT_SIZE));
        attributes.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int width = getWidth();
        final int right = width - paddingRight;
        final int height = getHeight();
        final int bottom = height - getPaddingBottom();
        if (mProgress <= 0) {
            canvas.drawRect(paddingLeft, paddingTop, right, bottom, mBackGroundPaint);
        } else if (mProgress >= mMaxProgress) {
            canvas.drawRect(paddingLeft, paddingTop, right, bottom, mFullProgressPaint);
        } else {
            final int middle = mProgress * (width - paddingLeft - paddingRight) / mMaxProgress;
            canvas.drawRect(paddingLeft, paddingTop, middle, bottom, mProgressPaint);
            canvas.drawRect(middle, paddingTop, right, bottom, mBackGroundPaint);
        }

        if (mText != null)
            canvas.drawText(mText, width - mTextPadding, height - mTextPadding, mTextPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY
                || (heightSpecMode != MeasureSpec.AT_MOST && heightSpecMode != MeasureSpec.UNSPECIFIED)) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        // we only use match_parent for width, and wrap_content for height
        final int height = (int) (mTextPaint.getTextSize()) + 2 * mTextPadding;
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                heightSpecMode == MeasureSpec.UNSPECIFIED ? height : Math.min(height, MeasureSpec.getSize(heightMeasureSpec)));
    }

    public void setProgress(int progress) {
        mProgress = progress;
        postInvalidate();
    }

    public int getMaxProgress() {
        return mMaxProgress;
    }

    public void setText(String text) {
        mText = text;
        postInvalidate();
    }
}
