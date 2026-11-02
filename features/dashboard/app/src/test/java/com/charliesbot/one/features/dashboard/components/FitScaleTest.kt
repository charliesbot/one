package com.charliesbot.one.features.dashboard.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FitScaleTest {

  @Test
  fun `content that fits keeps its size`() {
    assertEquals(1f, fitScale(contentWidth = 150, maxWidth = 160), 0f)
    assertEquals(1f, fitScale(contentWidth = 160, maxWidth = 160), 0f)
  }

  @Test
  fun `content wider than the space shrinks to fit exactly`() {
    // "20:00:54" at full size is wider than the dial's center.
    assertEquals(0.8f, fitScale(contentWidth = 200, maxWidth = 160), 0.0001f)
  }

  @Test
  fun `unbounded or empty space never scales`() {
    assertEquals(1f, fitScale(contentWidth = 200, maxWidth = Int.MAX_VALUE), 0f)
    assertEquals(1f, fitScale(contentWidth = 0, maxWidth = 160), 0f)
  }
}
