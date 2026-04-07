package com.redn.farm.data.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Converts a point-in-time epoch millis value to the start or end of the local
 * business day, used to scope all EOD queries consistently.
 */
object DateWindowHelper {

    /** Midnight (00:00:00.000) of the local day containing [millis]. */
    fun startOfDay(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    /** 23:59:59.999 of the local day containing [millis]. */
    fun endOfDay(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return date.atStartOfDay(zone).toInstant().toEpochMilli() + DAY_MILLIS - 1
    }

    /** Epoch millis for the start of today (local time). */
    fun todayStart(zone: ZoneId = ZoneId.systemDefault()): Long =
        startOfDay(System.currentTimeMillis(), zone)

    /** Epoch millis for the end of today (local time). */
    fun todayEnd(zone: ZoneId = ZoneId.systemDefault()): Long =
        endOfDay(System.currentTimeMillis(), zone)

    /** Epoch millis for midnight of a given [LocalDate]. */
    fun startOfDate(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
}
