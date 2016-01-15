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

package net.zionsoft.obadiah.ui.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import net.zionsoft.obadiah.R;

public class ProgressBar extends View {
    private static final int DEFAULT_MAX_PROGRESS = 100;
    private static final int DEFAULT_TEXT_PADDING = 0;
    private static final int DEFAULT_TEXT_SIZE = 20;

    // TODO introduces custom attributes for them
    private Paint backGroundPaint;
    private Paint progressPaint;
    private Paint fullProgressPaint;
    private Paint textPaint;

    private int progress;
    private int maxProgress = DEFAULT_MAX_PROGRESS;
    private int textPadding = DEFAULT_TEXT_PADDING;
    private String text;

    public ProgressBar(Context context) {
        super(context);
        init(context, null);
    }

    public ProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        final Resources resources = context.getResources();
        backGroundPaint = new Paint();
        backGroundPaint.setColor(Color.LTGRAY);
        progressPaint = new Paint();
        progressPaint.setColor(resources.getColor(R.color.dark_cyan));
        fullProgressPaint = new Paint();
        fullProgressPaint.setColor(resources.getColor(R.color.dark_lime));
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(DEFAULT_TEXT_SIZE);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        if (attrs != null) {
            final TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.ProgressBar, 0, 0);
            maxProgress = attributes.getInt(R.styleable.ProgressBar_maxProgress, DEFAULT_MAX_PROGRESS);
            textPadding = attributes.getDimensionPixelSize(R.styleable.ProgressBar_textPadding, DEFAULT_TEXT_PADDING);
            textPaint.setTextSize(attributes.getDimensionPixelSize(R.styleable.ProgressBar_textSize, DEFAULT_TEXT_SIZE));
            attributes.recycle();
        }
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
        if (progress <= 0) {
            canvas.drawRect(paddingLeft, paddingTop, right, bottom, backGroundPaint);
        } else if (progress >= maxProgress) {
            canvas.drawRect(paddingLeft, paddingTop, right, bottom, fullProgressPaint);
        } else {
            final int middle = progress * (width - paddingLeft - paddingRight) / maxProgress;
            canvas.drawRect(paddingLeft, paddingTop, middle, bottom, progressPaint);
            canvas.drawRect(middle, paddingTop, right, bottom, backGroundPaint);
        }

        if (text != null)
            canvas.drawText(text, width - textPadding, height - textPadding, textPaint);
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
        final int height = (int) (textPaint.getTextSize()) + 2 * textPadding;
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                heightSpecMode == MeasureSpec.UNSPECIFIED ? height : Math.min(height, MeasureSpec.getSize(heightMeasureSpec)));
    }

    public void setProgress(int progress) {
        this.progress = progress;
        postInvalidate();
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public void setText(String text) {
        this.text = text;
        postInvalidate();
    }
}
