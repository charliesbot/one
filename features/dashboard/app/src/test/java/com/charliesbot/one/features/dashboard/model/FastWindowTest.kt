package com.charliesbot.one.features.dashboard.model

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FastWindowTest {

  private val zone = ZoneId.of("America/Los_Angeles")
  private val sixteenHours = TimeUnit.HOURS.toMillis(16)

  private fun millis(dateTime: String): Long =
    LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

  @Test
  fun `end time is start plus goal duration`() {
    val window = FastWindow.create(millis("2026-10-23T18:00"), sixteenHours, null, zone)

    assertEquals(millis("2026-10-24T10:00"), window.endMillis)
  }

  @Test
  fun `bedtime inside the window is placed as a fraction of it`() {
    val window = FastWindow.create(millis("2026-10-23T18:00"), sixteenHours, 22 * 60, zone)

    assertEquals(millis("2026-10-23T22:00"), window.bedtimeMillis)
    assertEquals(0.25f, window.bedtimeFraction!!, 0.0001f)
  }

  @Test
  fun `bedtime already passed on the start day is looked up on the next day`() {
    // 22:00 the next day is 23h after a 23:00 start, past the 16h window.
    val window = FastWindow.create(millis("2026-10-23T23:00"), sixteenHours, 22 * 60, zone)

    assertNull(window.bedtimeMillis)
  }

  @Test
  fun `bedtime already passed on the start day can still land in a long window`() {
    val window =
      FastWindow.create(millis("2026-10-23T23:00"), TimeUnit.HOURS.toMillis(36), 22 * 60, zone)

    assertEquals(millis("2026-10-24T22:00"), window.bedtimeMillis)
  }

  @Test
  fun `bedtime after midnight is found on the next day`() {
    val window = FastWindow.create(millis("2026-10-23T20:00"), sixteenHours, 60, zone)

    assertEquals(millis("2026-10-24T01:00"), window.bedtimeMillis)
    assertEquals(5f / 16f, window.bedtimeFraction!!, 0.0001f)
  }

  @Test
  fun `bedtime outside a short window is null`() {
    val window =
      FastWindow.create(millis("2026-10-23T06:00"), TimeUnit.HOURS.toMillis(13), 22 * 60, zone)

    assertNull(window.bedtimeMillis)
    assertNull(window.bedtimeFraction)
  }

  @Test
  fun `progress and remaining time follow the clock`() {
    val window = FastWindow.create(millis("2026-10-22T20:30"), sixteenHours, null, zone)
    val now = millis("2026-10-23T06:12")

    assertEquals(9.7f / 16f, window.progressAt(now), 0.0001f)
    assertEquals(TimeUnit.MINUTES.toMillis(6 * 60 + 18), window.remainingAt(now))
  }

  @Test
  fun `progress stops at the goal and remaining time at zero`() {
    val window = FastWindow.create(millis("2026-10-22T20:30"), sixteenHours, null, zone)
    val now = millis("2026-10-23T20:00")

    assertEquals(1f, window.progressAt(now), 0f)
    assertEquals(0L, window.remainingAt(now))
  }

  @Test
  fun `progress before the start is zero`() {
    val window = FastWindow.create(millis("2026-10-23T18:00"), sixteenHours, null, zone)

    assertEquals(0f, window.progressAt(millis("2026-10-23T15:45")), 0f)
  }

  @Test
  fun `time past the goal is zero until the goal is reached`() {
    val window = FastWindow.create(millis("2026-10-22T22:25"), sixteenHours, null, zone)

    assertEquals(0L, window.overtimeAt(millis("2026-10-23T14:00")))
    assertEquals(0L, window.overtimeAt(millis("2026-10-23T14:25")))
  }

  @Test
  fun `time past the goal grows after it is reached`() {
    val window = FastWindow.create(millis("2026-10-22T22:25"), sixteenHours, null, zone)

    assertEquals(TimeUnit.HOURS.toMillis(4), window.overtimeAt(millis("2026-10-23T18:25")))
  }
}
