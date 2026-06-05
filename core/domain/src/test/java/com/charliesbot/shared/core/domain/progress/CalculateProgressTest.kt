package com.charliesbot.shared.core.domain.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalculateProgressTest {

  @Test
  fun `progress fraction is clamped between zero and one`() {
    assertEquals(0f, calculateProgressFraction(-1L, 100L))
    assertEquals(0.5f, calculateProgressFraction(50L, 100L))
    assertEquals(1f, calculateProgressFraction(150L, 100L))
  }

  @Test
  fun `progress percentage truncates fraction percentage`() {
    assertEquals(33, calculateProgressPercentage(1L, 3L))
  }

  @Test
  fun `progress fraction rejects null goal duration`() {
    val error = assertThrows(Error::class.java) { calculateProgressFraction(1L, null) }

    assertEquals("totalDurationGoalMillis cannot be null or less than or equal to 0", error.message)
  }

  @Test
  fun `progress percentage rejects non positive goal duration`() {
    val error = assertThrows(Error::class.java) { calculateProgressPercentage(1L, 0L) }

    assertEquals("totalDurationGoalMillis cannot be null or less than or equal to 0", error.message)
  }
}
