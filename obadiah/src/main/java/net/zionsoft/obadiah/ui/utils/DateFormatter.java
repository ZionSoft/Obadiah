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

package net.zionsoft.obadiah.ui.utils;

import android.content.res.Resources;

import net.zionsoft.obadiah.R;

import java.util.Calendar;

public class DateFormatter {
    private final Resources resources;
    private final String[] months;
    private final Calendar calendar;
    private final int currentYear;

    public DateFormatter(Resources resources) {
        super();

        this.resources = resources;
        months = resources.getStringArray(R.array.text_months);
        calendar = Calendar.getInstance();
        currentYear = calendar.get(Calendar.YEAR);
    }

    public String format(long timestamp) {
        calendar.setTimeInMillis(timestamp);
        final int year = calendar.get(Calendar.YEAR);
        if (year == currentYear) {
            return resources.getString(R.string.text_date_without_year,
                    calendar.get(Calendar.DATE), months[calendar.get(Calendar.MONTH)]);
        } else {
            return resources.getString(R.string.text_date, calendar.get(Calendar.DATE),
                    months[calendar.get(Calendar.MONTH)], year);
        }
    }
}
