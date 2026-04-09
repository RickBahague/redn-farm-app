package com.redn.farm.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MillisDateRangeTest {

    @Test
    fun contains_unbounded_alwaysTrue() {
        val mid = MillisDateRange.startOfDayMillis(1_700_000_000_000L)
        assertTrue(MillisDateRange.contains(mid, null to null))
    }

    @Test
    fun contains_inclusiveDayBounds() {
        val day = MillisDateRange.startOfDayMillis(1_700_000_000_000L)
        val start = MillisDateRange.startOfDayMillis(day)
        val end = MillisDateRange.endOfDayMillis(day)
        assertTrue(MillisDateRange.contains(start, start to end))
        assertTrue(MillisDateRange.contains(end, start to end))
        assertFalse(MillisDateRange.contains(start - 1, start to end))
        assertFalse(MillisDateRange.contains(end + 1, start to end))
    }
}
