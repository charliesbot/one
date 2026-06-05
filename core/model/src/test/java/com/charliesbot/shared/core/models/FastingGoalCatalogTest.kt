package com.charliesbot.shared.core.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastingGoalCatalogTest {

  @Test
  fun `allGoals contains predefined goals with correct durations`() {
    val goals = FastingGoalCatalog.allGoals

    assertEquals(5, goals.size)
    assertEquals(13L * MILLIS_PER_HOUR, FastingGoalCatalog.CIRCADIAN.durationMillis)
    assertEquals(16L * MILLIS_PER_HOUR, FastingGoalCatalog.SIXTEEN_EIGHT.durationMillis)
    assertEquals(18L * MILLIS_PER_HOUR, FastingGoalCatalog.EIGHTEEN_SIX.durationMillis)
    assertEquals(20L * MILLIS_PER_HOUR, FastingGoalCatalog.TWENTY_FOUR.durationMillis)
    assertEquals(36L * MILLIS_PER_HOUR, FastingGoalCatalog.THIRTY_SIX_HOUR.durationMillis)
  }

  @Test
  fun `getGoalById resolves predefined ids`() {
    assertEquals(FastingGoalCatalog.CIRCADIAN, FastingGoalCatalog.getGoalById("circadian"))
    assertEquals(FastingGoalCatalog.SIXTEEN_EIGHT, FastingGoalCatalog.getGoalById("16:8"))
    assertEquals(FastingGoalCatalog.EIGHTEEN_SIX, FastingGoalCatalog.getGoalById("18:6"))
    assertEquals(FastingGoalCatalog.TWENTY_FOUR, FastingGoalCatalog.getGoalById("20:4"))
    assertEquals(FastingGoalCatalog.THIRTY_SIX_HOUR, FastingGoalCatalog.getGoalById("36hour"))
  }

  @Test
  fun `getGoalById falls back to default goal for unknown ids`() {
    assertEquals(FastingGoalCatalog.SIXTEEN_EIGHT, FastingGoalCatalog.getGoalById("unknown"))
  }

  @Test
  fun `fasting goal exposes display duration and custom flag without android types`() {
    val customGoal =
      FastingGoal(id = "custom_1", name = "My Goal", durationMillis = 14L * MILLIS_PER_HOUR)

    assertEquals("14", customGoal.durationDisplay)
    assertTrue(customGoal.isCustom)
    assertFalse(FastingGoalCatalog.SIXTEEN_EIGHT.isCustom)
  }
}

private const val MILLIS_PER_HOUR = 60L * 60L * 1000L
