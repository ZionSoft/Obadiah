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

package net.zionsoft.obadiah.utils;

import java.util.Formatter;
import java.util.Locale;

public class TextFormatter {
    private static final StringBuilder FORMATTER_BUFFER = new StringBuilder();
    private static final Formatter FORMATTER = new Formatter(FORMATTER_BUFFER, Locale.getDefault());

    public static String format(String format, Object... args) {
        synchronized (FORMATTER) {
            FORMATTER_BUFFER.setLength(0);
            return FORMATTER.format(format, args).toString();
        }
    }
}
