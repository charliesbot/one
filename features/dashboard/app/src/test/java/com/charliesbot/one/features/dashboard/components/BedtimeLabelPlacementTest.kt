package com.charliesbot.one.features.dashboard.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BedtimeLabelPlacementTest {

  private fun place(markerX: Int, startWidth: Int = 100, endWidth: Int = 100) =
    bedtimeLabelX(
      markerX = markerX,
      labelWidth = 100,
      startWidth = startWidth,
      endWidth = endWidth,
      totalWidth = 1000,
      gap = 16,
    )

  @Test
  fun `label centers on its marker when there is room`() {
    assertEquals(450, place(markerX = 500))
  }

  @Test
  fun `label slides right to clear a wide start time`() {
    // Centered it would start at 130, inside the 140-wide start label.
    assertEquals(156, place(markerX = 180, startWidth = 140))
  }

  @Test
  fun `label slides left to clear a wide end time`() {
    assertEquals(744, place(markerX = 820, endWidth = 140))
  }

  @Test
  fun `label hides when clearing the start time would leave its marker behind`() {
    assertNull(place(markerX = 120, startWidth = 140))
  }

  @Test
  fun `label hides when it does not fit between the edge labels`() {
    assertNull(
      bedtimeLabelX(
        markerX = 150,
        labelWidth = 100,
        startWidth = 120,
        endWidth = 120,
        totalWidth = 300,
        gap = 16,
      )
    )
  }
}
