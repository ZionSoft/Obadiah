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

package net.zionsoft.obadiah.biblereading.verse;

import org.junit.Assert;
import org.junit.Test;

public class VerseHelperTest {
    @Test
    public void testPositionToBookIndex() {
        Assert.assertEquals(0, VerseHelper.positionToBookIndex(0));
        Assert.assertEquals(0, VerseHelper.positionToBookIndex(49));

        Assert.assertEquals(1, VerseHelper.positionToBookIndex(50));
        Assert.assertEquals(1, VerseHelper.positionToBookIndex(55));

        Assert.assertEquals(65, VerseHelper.positionToBookIndex(1188));
    }

    @Test
    public void testPositionToChapterIndex() {
        Assert.assertEquals(0, VerseHelper.positionToChapterIndex(0));
        Assert.assertEquals(49, VerseHelper.positionToChapterIndex(49));

        Assert.assertEquals(0, VerseHelper.positionToChapterIndex(50));
        Assert.assertEquals(5, VerseHelper.positionToChapterIndex(55));

        Assert.assertEquals(21, VerseHelper.positionToChapterIndex(1188));
    }

    @Test
    public void testIndexToPosition() {
        Assert.assertEquals(0, VerseHelper.indexToPosition(0, 0));
        Assert.assertEquals(49, VerseHelper.indexToPosition(0, 49));

        Assert.assertEquals(50, VerseHelper.indexToPosition(1, 0));
        Assert.assertEquals(55, VerseHelper.indexToPosition(1, 5));

        Assert.assertEquals(1188, VerseHelper.indexToPosition(65, 21));
    }
}
