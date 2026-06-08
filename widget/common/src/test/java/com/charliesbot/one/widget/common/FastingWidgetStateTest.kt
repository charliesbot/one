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
}

private val Int.hoursMillis: Long
  get() = this * 60L * 60L * 1000L
