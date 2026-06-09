package com.charliesbot.one.widget.wear

import com.charliesbot.one.widget.common.FastingWidgetState
import org.junit.Assert.assertEquals
import org.junit.Test

class WearWidgetContentTest {
  @Test
  fun `returns not fasting content when no fast is active`() {
    val content = notFastingState().toWearWidgetContent()

    assertEquals(WearWidgetContent(primaryText = "Not fasting", secondaryText = ""), content)
  }

  @Test
  fun `returns remaining hours content while fasting`() {
    val content = fastingState(hoursRemaining = 4).toWearWidgetContent()

    assertEquals(WearWidgetContent(primaryText = "4", secondaryText = "hours left: 4"), content)
  }

  @Test
  fun `returns goal met content after the goal is reached`() {
    val content = fastingState(hoursRemaining = -1, isGoalMet = true).toWearWidgetContent()

    assertEquals(WearWidgetContent(primaryText = "Goal", secondaryText = "Met!"), content)
  }

  private fun FastingWidgetState.toWearWidgetContent(): WearWidgetContent =
    toWearWidgetContent(
      notFastingText = "Not fasting",
      goalMetPartOne = "Goal",
      goalMetPartTwo = "Met!",
      hoursLeftText = { "hours left: $it" },
    )

  private fun notFastingState() =
    FastingWidgetState(
      isFasting = false,
      elapsedMillis = 0,
      fastingGoalMillis = 16.hoursMillis,
      hoursRemaining = 16,
      isGoalMet = false,
      progressFraction = 0f,
    )

  private fun fastingState(hoursRemaining: Long, isGoalMet: Boolean = false) =
    FastingWidgetState(
      isFasting = true,
      elapsedMillis = ((16 - hoursRemaining).coerceAtLeast(0) * MILLIS_PER_HOUR),
      fastingGoalMillis = 16.hoursMillis,
      hoursRemaining = hoursRemaining,
      isGoalMet = isGoalMet,
      progressFraction = 0.75f,
    )

  private val Int.hoursMillis: Long
    get() = this * 60L * 60L * 1000L

  private companion object {
    private const val MILLIS_PER_HOUR = 60L * 60L * 1000L
  }
}
