package com.charliesbot.one.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class OneWidgetSizeTest {
  @Test
  fun wideShortSizeUsesWideLayout() {
    assertEquals(OneWidgetLayoutSize.Wide, OneWidgetSize.layoutFor(DpSize(245.dp, 56.dp)))
  }
}
