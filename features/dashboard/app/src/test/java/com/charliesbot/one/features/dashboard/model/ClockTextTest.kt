package com.charliesbot.one.features.dashboard.model

import java.time.LocalTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockTextTest {

  @Test
  fun `twelve hour clock splits the period`() {
    assertEquals(
      ClockText(time = "6:00", period = "PM"),
      ClockText.of(LocalTime.of(18, 0), is24Hour = false, Locale.US),
    )
  }

  @Test
  fun `twenty four hour clock has no period`() {
    assertEquals(
      ClockText(time = "18:00", period = null),
      ClockText.of(LocalTime.of(18, 0), is24Hour = true, Locale.US),
    )
  }

  @Test
  fun `compact clock drops zero minutes on twelve hour clocks`() {
    assertEquals("6 PM", ClockText.compact(LocalTime.of(18, 0), is24Hour = false, Locale.US))
    assertEquals("8:30 PM", ClockText.compact(LocalTime.of(20, 30), is24Hour = false, Locale.US))
    assertEquals("18:00", ClockText.compact(LocalTime.of(18, 0), is24Hour = true, Locale.US))
  }

  @Test
  fun `hours and minutes split a duration`() {
    assertEquals(HoursMinutes(6, 18), HoursMinutes.of(((6 * 60 + 18) * 60_000L) + 59_000L))
    assertEquals(HoursMinutes(0, 0), HoursMinutes.of(-5_000L))
  }

  @Test
  fun `time left rounds up to the next minute so it never reads zero early`() {
    assertEquals(HoursMinutes(0, 1), HoursMinutes.roundedUp(30_000L))
    assertEquals(HoursMinutes(6, 18), HoursMinutes.roundedUp(((6 * 60 + 17) * 60_000L) + 1L))
    assertEquals(HoursMinutes(6, 18), HoursMinutes.roundedUp((6 * 60 + 18) * 60_000L))
    assertEquals(HoursMinutes(0, 0), HoursMinutes.roundedUp(0L))
  }
}
