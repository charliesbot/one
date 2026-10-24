package com.charliesbot.one.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BedtimeMarkerTest {

  @get:Rule val composeRule = createComposeRule()

  /** The marker's painted columns along the bar's middle row, in dp: first to last. */
  private fun markerSpan(fraction: Float): ClosedFloatingPointRange<Float> {
    composeRule.setContent {
      Box(Modifier.size(width = 300.dp, height = 16.dp).background(Color.White)) {
        BedtimeMarker(Color.Red, fraction)
      }
    }
    val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
    val density = bitmap.width / 300f
    val row = bitmap.height / 2
    val red = (0 until bitmap.width).filter { bitmap.getPixel(it, row) == Color.Red.toArgb() }
    return (red.first() / density)..((red.last() + 1) / density)
  }

  @Test
  fun markerCentersOnItsFraction() {
    val span = markerSpan(fraction = 0.5f)

    assertEquals(146f, span.start, 1f)
    assertEquals(154f, span.endInclusive, 1f)
  }

  @Test
  fun markerAtTheStartKeepsItsSize() {
    // A fast that starts at bedtime: the dot's center is the bar's start, its right half visible.
    val span = markerSpan(fraction = 0f)

    assertEquals(0f, span.start, 1f)
    assertEquals(4f, span.endInclusive, 1f)
  }
}
