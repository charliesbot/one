package com.charliesbot.shared.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    // ============= Existing DateUtils Function Tests =============

    @Test
    fun `getHours returns correct hours from milliseconds`() {
        val oneHourMillis = 1 * 60 * 60 * 1000L
        assertEquals(1, getHours(oneHourMillis))

        val sixteenHoursMillis = 16 * 60 * 60 * 1000L
        assertEquals(16, getHours(sixteenHoursMillis))

        val zeroMillis = 0L
        assertEquals(0, getHours(zeroMillis))
    }

    @Test
    fun `getHours handles null input`() {
        assertEquals(0, getHours(null))
    }

    @Test
    fun `formatTimestamp formats duration correctly`() {
        // 1 hour, 30 minutes, 45 seconds
        val millis = (1 * 60 * 60 + 30 * 60 + 45) * 1000L
        val result = formatTimestamp(millis)
        assertEquals("01:30:45", result)
    }

    @Test
    fun `formatTimestamp handles zero`() {
        val result = formatTimestamp(0L)
        assertEquals("00:00:00", result)
    }

    @Test
    fun `formatTimestamp handles large values`() {
        // 100 hours, 30 minutes, 15 seconds
        val millis = (100 * 60 * 60 + 30 * 60 + 15) * 1000L
        val result = formatTimestamp(millis)
        assertEquals("100:30:15", result)
    }

    @Test
    fun `convertMillisToLocalDateTime returns valid LocalDateTime`() {
        val millis = System.currentTimeMillis()
        val result = convertMillisToLocalDateTime(millis)
        assertNotNull(result)
    }

    @Test
    fun `convertLocalDateTimeToMillis and back is consistent`() {
        val original = System.currentTimeMillis()
        val localDateTime = convertMillisToLocalDateTime(original)
        val converted = convertLocalDateTimeToMillis(localDateTime)

        // Should be within 1 second (millisecond precision may differ)
        assertTrue(
            "Expected times to be close but diff was ${kotlin.math.abs(original - converted)}",
            kotlin.math.abs(original - converted) < 1000
        )
    }

}
