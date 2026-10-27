package com.charliesbot.one.features.dashboard.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * The uniform scale that makes [contentWidth] fit in [maxWidth]: 1 when it already fits or the
 * space is unbounded, smaller otherwise.
 */
internal fun fitScale(contentWidth: Int, maxWidth: Int): Float =
  if (maxWidth == Int.MAX_VALUE || contentWidth <= maxWidth || contentWidth == 0) {
    1f
  } else {
    maxWidth.toFloat() / contentWidth
  }

/**
 * Shrinks this element uniformly when it is wider than the space it gets, instead of letting it
 * clip or wrap. Big numbers keep their single line and full digits, like a timer that grows past
 * "99:59" or a time shown at a large font scale.
 */
internal fun Modifier.scaleToFitWidth(): Modifier = layout { measurable, constraints ->
  val placeable =
    measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
  val scale = fitScale(placeable.width, constraints.maxWidth)
  val width =
    (placeable.width * scale).roundToInt().coerceIn(constraints.minWidth, constraints.maxWidth)
  val height = (placeable.height * scale).roundToInt()
  layout(width, height) {
    // The layer scales around the placeable's center, so center it in the fitted box.
    placeable.placeWithLayer((width - placeable.width) / 2, (height - placeable.height) / 2) {
      scaleX = scale
      scaleY = scale
    }
  }
}
