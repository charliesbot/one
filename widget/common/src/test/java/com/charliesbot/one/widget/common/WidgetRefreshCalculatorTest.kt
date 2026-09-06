package com.charliesbot.one.widget.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetRefreshCalculatorTest {
  private val oneHourMillis = 60L * 60L * 1000L

  @Test
  fun `fast at 11 hours into 16 hour goal schedules exactly next hour boundary`() {
    val startTime = 1000000L
    // 11 hours and 15 minutes elapsed (4 hours 45 mins left)
    val currentTime = startTime + (11 * oneHourMillis) + (15 * 60 * 1000L)
    val goalDuration = 16 * oneHourMillis

    // Next boundary should be at 12 hours elapsed (45 minutes from now)
    val expectedDelay = 45 * 60 * 1000L
    val actualDelay = WidgetRefreshCalculator.calculateNextRefreshDelayMillis(
      currentTimeMillis = currentTime,
      startTimeMillis = startTime,
      goalDurationMillis = goalDuration,
    )

    assertEquals(expectedDelay, actualDelay)
  }

  @Test
  fun `advancing time from 5 hours left to 2 hours left calculates correct remaining delay without state change`() {
    val startTime = 0L
    val goalDuration = 16 * oneHourMillis

    // At 11 hours elapsed: 5 hours remaining
    val timeAtFiveHoursLeft = 11 * oneHourMillis
    val delayAtFiveHours = WidgetRefreshCalculator.calculateNextRefreshDelayMillis(
      currentTimeMillis = timeAtFiveHoursLeft,
      startTimeMillis = startTime,
      goalDurationMillis = goalDuration,
    )
    // When exactly on the hour, next tick is 1 full hour later
    assertEquals(oneHourMillis, delayAtFiveHours)

    // Time advances 3 hours to 14 hours elapsed (2 hours remaining) without DB update
    val timeAtTwoHoursLeft = 14 * oneHourMillis + (20 * 60 * 1000L)
    val state = com.charliesbot.shared.core.models.FastingDataItem(isFasting = true, startTimeInMillis = startTime)
      .toFastingWidgetState(timeAtTwoHoursLeft, goalDuration)

    assertEquals(2L, state.hoursRemaining)
    assertEquals(0.8958f, state.progressFraction, 0.001f)

    val delayAtTwoHours = WidgetRefreshCalculator.calculateNextRefreshDelayMillis(
      currentTimeMillis = timeAtTwoHoursLeft,
      startTimeMillis = startTime,
      goalDurationMillis = goalDuration,
    )
    // 40 minutes remaining until 15th hour
    assertEquals(40 * 60 * 1000L, delayAtTwoHours)
  }

  @Test
  fun `delay caps at goal boundary when remaining time is less than one hour`() {
    val startTime = 0L
    val goalDuration = 16 * oneHourMillis
    // 15 hours and 40 minutes elapsed -> 20 minutes remaining until goal
    val currentTime = 15 * oneHourMillis + (40 * 60 * 1000L)

    val actualDelay = WidgetRefreshCalculator.calculateNextRefreshDelayMillis(
      currentTimeMillis = currentTime,
      startTimeMillis = startTime,
      goalDurationMillis = goalDuration,
    )

    // Delay must cap at 20 minutes (goal reached), not 60 minutes
    assertEquals(20 * 60 * 1000L, actualDelay)
  }

  @Test
  fun `returns null when goal is already reached or exceeded`() {
    val startTime = 0L
    val goalDuration = 16 * oneHourMillis
    val currentTime = 16 * oneHourMillis

    val actualDelay = WidgetRefreshCalculator.calculateNextRefreshDelayMillis(
      currentTimeMillis = currentTime,
      startTimeMillis = startTime,
      goalDurationMillis = goalDuration,
    )

    assertNull(actualDelay)
  }

  @Test
  fun `returns time until start if current time is before start time`() {
    val startTime = 5000L
    val currentTime = 1000L
    val goalDuration = 16 * oneHourMillis

    val actualDelay = WidgetRefreshCalculator.calculateNextRefreshDelayMillis(
      currentTimeMillis = currentTime,
      startTimeMillis = startTime,
      goalDurationMillis = goalDuration,
    )

    assertEquals(4000L, actualDelay)
  }
}
