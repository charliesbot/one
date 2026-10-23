package com.charliesbot.one.features.dashboard.components

import org.junit.Assert.assertEquals
import org.junit.Test

class BedtimeLabelPlacementTest {

  private fun place(markerX: Int) =
    bedtimeLabelX(markerX = markerX, labelWidth = 100, totalWidth = 1000)

  @Test
  fun `label centers on its marker when there is room`() {
    assertEquals(450, place(markerX = 500))
  }

  @Test
  fun `label near the start stays inside the bar instead of hiding`() {
    // Bedtime half an hour into a fast: centered it would start before the bar.
    assertEquals(0, place(markerX = 20))
  }

  @Test
  fun `label near the end stays inside the bar instead of hiding`() {
    assertEquals(900, place(markerX = 990))
  }

  @Test
  fun `label wider than the bar starts at its start`() {
    assertEquals(0, bedtimeLabelX(markerX = 50, labelWidth = 120, totalWidth = 100))
  }
}
