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

package net.zionsoft.obadiah.model.utils;

import android.content.res.Resources;

import net.zionsoft.obadiah.R;

import java.util.Calendar;

public class DateFormatter {
    private final Resources mResources;
    private final String[] mMonths;
    private final Calendar mCalendar;
    private final int mCurrentYear;

    public DateFormatter(Resources resources) {
        super();

        mResources = resources;
        mMonths = resources.getStringArray(R.array.text_months);
        mCalendar = Calendar.getInstance();
        mCurrentYear = mCalendar.get(Calendar.YEAR);
    }

    public String format(long timestamp) {
        mCalendar.setTimeInMillis(timestamp);
        final int year = mCalendar.get(Calendar.YEAR);
        if (year == mCurrentYear) {
            return mResources.getString(R.string.text_date_without_year,
                    mCalendar.get(Calendar.DATE), mMonths[mCalendar.get(Calendar.MONTH)]);
        } else {
            return mResources.getString(R.string.text_date, mCalendar.get(Calendar.DATE),
                    mMonths[mCalendar.get(Calendar.MONTH)], year);
        }
    }
}
