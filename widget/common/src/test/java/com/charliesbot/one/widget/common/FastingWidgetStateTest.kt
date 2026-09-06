package com.charliesbot.one.widget.common

import com.charliesbot.shared.core.models.FastingDataItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastingWidgetStateTest {
  @Test
  fun activeFastMapsProgressAndRemainingHours() {
    val state =
      FastingDataItem(isFasting = true, startTimeInMillis = 0L)
        .toFastingWidgetState(
          currentTimeMillis = 12.hoursMillis,
          fastingGoalMillis = 16.hoursMillis,
        )

    assertTrue(state.isFasting)
    assertEquals(12.hoursMillis, state.elapsedMillis)
    assertEquals(16.hoursMillis, state.fastingGoalMillis)
    assertEquals(4, state.hoursRemaining)
    assertFalse(state.isGoalMet)
    assertEquals(0.75f, state.progressFraction, 0.0001f)
  }

  @Test
  fun completedFastMarksGoalMet() {
    val state =
      FastingDataItem(isFasting = true, startTimeInMillis = 0L)
        .toFastingWidgetState(
          currentTimeMillis = 17.hoursMillis,
          fastingGoalMillis = 16.hoursMillis,
        )

    assertTrue(state.isGoalMet)
    assertEquals(-1, state.hoursRemaining)
    assertEquals(1f, state.progressFraction, 0.0001f)
  }

  @Test
  fun inactiveFastHasZeroProgress() {
    val state =
      FastingDataItem(isFasting = false, startTimeInMillis = 0L)
        .toFastingWidgetState(
          currentTimeMillis = 12.hoursMillis,
          fastingGoalMillis = 16.hoursMillis,
        )

    assertFalse(state.isFasting)
    assertEquals(0L, state.elapsedMillis)
    assertEquals(16, state.hoursRemaining)
    assertFalse(state.isGoalMet)
    assertEquals(0f, state.progressFraction, 0.0001f)
  }

  @Test
  fun customGoalDurationMapsAccuratelyAndRecalculatesOnTimeAdvance() {
    val startTime = 0L
    val customGoal14h = 14.hoursMillis

    // At 9 hours elapsed -> 5 hours left
    val state9h =
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = "custom_14")
        .toFastingWidgetState(
          currentTimeMillis = 9.hoursMillis,
          fastingGoalMillis = customGoal14h,
        )
    assertEquals(5, state9h.hoursRemaining)
    assertEquals(9f / 14f, state9h.progressFraction, 0.0001f)

    // Time advances to 12 hours elapsed (delayed execution) -> 2 hours left
    val state12h =
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = "custom_14")
        .toFastingWidgetState(
          currentTimeMillis = 12.hoursMillis,
          fastingGoalMillis = customGoal14h,
        )
    assertEquals(2, state12h.hoursRemaining)
    assertEquals(12f / 14f, state12h.progressFraction, 0.0001f)
  }
}

private val Int.hoursMillis: Long
  get() = this * 60L * 60L * 1000L
