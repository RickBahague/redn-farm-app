package com.redn.farm.data.local.converters

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Covers BACKLOG DI-05 — inconsistent date storage (epoch seconds vs millis).
 *
 * The converter stores LocalDateTime as epoch SECONDS (UTC).
 * Entities that also store dates as Long use System.currentTimeMillis() which
 * returns epoch MILLISECONDS.  These two must never be mixed.
 *
 * Run with: ./gradlew :app:testDebugUnitTest --tests "*.DateTimeConverterTest"
 */
class DateTimeConverterTest {

    private val converter = DateTimeConverter()

    @Test
    fun roundTrip_dateTimeIsPreserved() {
        val original = LocalDateTime.of(2024, 3, 15, 10, 30, 0)
        val stored = converter.dateToTimestamp(original)
        val restored = converter.fromTimestamp(stored)
        assertEquals(original, restored)
    }

    @Test
    fun dateToTimestamp_producesEpochSeconds_notMillis() {
        // 2024-01-01 00:00:00 UTC = 1704067200 seconds = 1_704_067_200_000 millis
        val date = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
        val stored = converter.dateToTimestamp(date)!!

        val expectedSeconds = date.toEpochSecond(ZoneOffset.UTC)
        val expectedMillis = expectedSeconds * 1000

        assertEquals("Converter must store epoch SECONDS", expectedSeconds, stored)

        // This assertion documents the DI-05 inconsistency:
        // if stored value were treated as millis it would be 1000x too large
        assertNotEquals(
            "Epoch seconds and epoch millis are NOT interchangeable (BACKLOG DI-05)",
            expectedMillis, stored
        )
    }

    @Test
    fun fromTimestamp_treatsValueAsEpochSeconds() {
        // 1704067200 = 2024-01-01 00:00:00 UTC in epoch seconds
        val epochSeconds = 1_704_067_200L
        val result = converter.fromTimestamp(epochSeconds)!!
        assertEquals(2024, result.year)
        assertEquals(1, result.monthValue)
        assertEquals(1, result.dayOfMonth)
    }

    @Test
    fun fromTimestamp_whenGivenMillis_producesWrongDate() {
        // This test DOCUMENTS the DI-05 bug.
        // System.currentTimeMillis() for 2024-01-01 = 1_704_067_200_000
        // If that millis value is fed into fromTimestamp() (which expects seconds),
        // the year will be ~55970 — clearly wrong.
        val epochMillis = 1_704_067_200_000L // what System.currentTimeMillis() would return
        val wrongResult = converter.fromTimestamp(epochMillis)!!
        assertTrue(
            "Feeding millis into a seconds-based converter produces an absurd year (${wrongResult.year}). " +
            "Fix entities that mix System.currentTimeMillis() with LocalDateTime columns. See BACKLOG DI-05.",
            wrongResult.year > 9999
        )
    }

    @Test
    fun nullHandling_roundTrip() {
        assertNull(converter.dateToTimestamp(null))
        assertNull(converter.fromTimestamp(null))
    }

    @Test
    fun roundTrip_preservesTimeComponent() {
        val original = LocalDateTime.of(2023, 6, 15, 14, 35, 47)
        val restored = converter.fromTimestamp(converter.dateToTimestamp(original))
        assertEquals(14, restored!!.hour)
        assertEquals(35, restored.minute)
        assertEquals(47, restored.second)
    }
}
