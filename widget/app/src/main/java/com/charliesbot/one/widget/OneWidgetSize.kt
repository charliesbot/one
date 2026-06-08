package com.charliesbot.one.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

internal enum class OneWidgetLayoutSize {
  Minimal,
  Compact,
  Wide,
  Expanded,
}

internal object OneWidgetSize {
  /** Below this square side, hide text and show the ring only. */
  val TEXT_THRESHOLD = 100.dp

  val Tiny = DpSize(57.dp, 57.dp)
  val Minimal = DpSize(109.dp, 56.dp)
  val Compact = DpSize(130.dp, 130.dp)
  val WideShort = DpSize(245.dp, 56.dp)
  val Wide = DpSize(245.dp, 115.dp)
  val Expanded = DpSize(300.dp, 180.dp)

  val SupportedSizes = setOf(Tiny, Minimal, Compact, WideShort, Wide, Expanded)

  fun layoutFor(size: DpSize): OneWidgetLayoutSize =
    when {
      size.width >= 300.dp && size.height >= 180.dp -> OneWidgetLayoutSize.Expanded
      size.width >= 180.dp && size.height >= 56.dp -> OneWidgetLayoutSize.Wide
      size.height < 80.dp -> OneWidgetLayoutSize.Minimal
      else -> OneWidgetLayoutSize.Compact
    }
}
