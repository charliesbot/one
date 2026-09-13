package com.charliesbot.shared.core.models

import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class ClockFormatTest {
  @Test
  fun `24 hour format handles midnight noon and evening`() {
    val format = ClockFormat(true, Locale.US)
    assertEquals("00:00", format.time(LocalTime.MIDNIGHT))
    assertEquals("12:00", format.time(LocalTime.NOON))
    assertEquals("18:30", format.time(LocalTime.of(18, 30)))
  }

  @Test
  fun `12 hour format handles midnight noon and evening`() {
    val format = ClockFormat(false, Locale.US)
    assertEquals("12:00 AM", format.time(LocalTime.MIDNIGHT))
    assertEquals("12:00 PM", format.time(LocalTime.NOON))
    assertEquals("6:30 PM", format.time(LocalTime.of(18, 30)))
  }

  @Test
  fun `system follows device but explicit choices override it`() {
    assertTrue(TimeFormatMode.SYSTEM.is24Hour(true))
    assertFalse(TimeFormatMode.SYSTEM.is24Hour(false))
    assertTrue(TimeFormatMode.TWENTY_FOUR_HOUR.is24Hour(false))
    assertFalse(TimeFormatMode.TWELVE_HOUR.is24Hour(true))
    assertEquals(TimeFormatMode.SYSTEM, TimeFormatMode.fromStoredValue("unknown"))
    assertEquals(TimeFormatMode.SYSTEM, TimeFormatMode.fromStoredValue(null))
  }
}
