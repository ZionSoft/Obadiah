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

package net.zionsoft.obadiah.search;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import net.zionsoft.obadiah.model.domain.VerseIndex;
import net.zionsoft.obadiah.model.domain.VerseSearchResult;

import java.util.Locale;

class SearchedVerse {
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();

    final VerseIndex index;

    private String query;
    private String bookName;
    private String text;
    private CharSequence textForDisplay;

    SearchedVerse(VerseSearchResult verseSearchResult, String query) {
        this.index = verseSearchResult.index;
        this.query = query;
        this.bookName = verseSearchResult.bookName;
        this.text = verseSearchResult.text;
    }

    CharSequence getTextForDisplay() {
        if (textForDisplay == null) {
            final SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(bookName).append(' ')
                    .append(Integer.toString(index.chapter() + 1))
                    .append(':')
                    .append(Integer.toString(index.verse() + 1))
                    .append('\n');
            final int startIndex = builder.length();
            builder.append(text);

            final String lowerCase = builder.toString().toLowerCase(DEFAULT_LOCALE);
            final String[] keywords = query.trim().replaceAll("\\s+", " ").split(" ");
            for (String keyword : keywords) {
                final int start = lowerCase.indexOf(keyword.toLowerCase(DEFAULT_LOCALE), startIndex);
                if (start >= 0) {
                    final int end = start + keyword.length();
                    builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    builder.setSpan(new RelativeSizeSpan(1.2F), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                }
            }

            query = null;
            bookName = null;
            text = null;
            textForDisplay = builder;
        }
        return textForDisplay;
    }
}
