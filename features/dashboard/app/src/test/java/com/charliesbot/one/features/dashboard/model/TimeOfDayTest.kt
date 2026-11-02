package com.charliesbot.one.features.dashboard.model

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeOfDayTest {

  private val zone = ZoneId.of("America/Los_Angeles")
  private val sixPm = 18 * 60

  private fun millis(dateTime: String): Long =
    LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

  @Test
  fun `next time of day later today is today`() {
    assertEquals(millis("2026-10-23T18:00"), nextTimeOfDay(sixPm, millis("2026-10-23T15:45"), zone))
  }

  @Test
  fun `next time of day already passed is tomorrow`() {
    assertEquals(millis("2026-10-24T18:00"), nextTimeOfDay(sixPm, millis("2026-10-23T18:01"), zone))
  }

  @Test
  fun `next time of day at the exact moment is that moment`() {
    assertEquals(millis("2026-10-23T18:00"), nextTimeOfDay(sixPm, millis("2026-10-23T18:00"), zone))
  }

  @Test
  fun `a time skipped by daylight saving resolves to the shifted local time`() {
    // 2:30 AM doesn't exist on 2026-03-08 in Los Angeles; clocks jump from 2:00 to 3:00.
    assertEquals(
      millis("2026-03-08T03:30"),
      nextTimeOfDay(2 * 60 + 30, millis("2026-03-08T01:00"), zone),
    )
  }

  @Test
  fun `suggested start stays on today while it is due`() {
    assertEquals(
      millis("2026-10-23T18:00"),
      suggestedStartMillis(sixPm, millis("2026-10-23T18:40"), zone),
    )
  }

  @Test
  fun `suggested start moves to tomorrow an hour after it passes`() {
    assertEquals(
      millis("2026-10-24T18:00"),
      suggestedStartMillis(sixPm, millis("2026-10-23T19:01"), zone),
    )
  }

  @Test
  fun `suggested start computed days ago still lands on the next one`() {
    // Opened Thursday, fasted until Friday morning: the next suggestion is Friday evening.
    assertEquals(
      millis("2026-10-24T18:00"),
      suggestedStartMillis(sixPm, millis("2026-10-24T10:00"), zone),
    )
  }
}
