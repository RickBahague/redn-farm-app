package com.redn.farm.utils

import java.time.Instant
import java.time.ZoneId

/** Inclusive calendar-day filtering and normalizers for BUG-ARC-09 (epoch millis in state). */
object MillisDateRange {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun startOfDayMillis(anyMillisOnDay: Long): Long =
        Instant.ofEpochMilli(anyMillisOnDay).atZone(zone).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDayMillis(anyMillisOnDay: Long): Long =
        Instant.ofEpochMilli(anyMillisOnDay).atZone(zone).toLocalDate()
            .atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()

    /** Inclusive range on calendar days; null bound = unbounded. */
    fun contains(valueMillis: Long, range: Pair<Long?, Long?>): Boolean {
        val (startRaw, endRaw) = range
        if (startRaw == null && endRaw == null) return true
        if (startRaw == null) return valueMillis <= endOfDayMillis(endRaw!!)
        if (endRaw == null) return valueMillis >= startOfDayMillis(startRaw)
        return valueMillis in startOfDayMillis(startRaw)..endOfDayMillis(endRaw)
    }
}
