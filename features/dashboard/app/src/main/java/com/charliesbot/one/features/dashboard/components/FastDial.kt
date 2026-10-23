package com.charliesbot.one.features.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ArcStroke = 20.dp

// The card's own pair has no quieter tone for an empty track, so the track is thin instead.
private val TrackStroke = 4.dp

/** Room between the dial's edge and its center content: the arc plus breathing room. */
private val ContentInset = ArcStroke + 28.dp

/**
 * A dial for one fast: a thick flat arc runs from the start (top) around to the goal (back at the
 * top), whatever the goal's length, over a thin track for the time still to go. [content] sits in
 * the clear center.
 *
 * [arcModifier] applies to the drawn arc but not to [content], so the dial can bloom in on its own.
 */
@Composable
internal fun FastDial(
  progress: () -> Float,
  size: Dp,
  modifier: Modifier = Modifier,
  arcModifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val trackColor = MaterialTheme.colorScheme.onPrimaryContainer
  val arcColor = MaterialTheme.colorScheme.primary

  Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
    Canvas(
      Modifier.fillMaxSize().then(arcModifier).semantics {
        progressBarRangeInfo = ProgressBarRangeInfo(progress().coerceIn(0f, 1f), 0f..1f)
      }
    ) {
      val strokePx = ArcStroke.toPx()
      val radiusPx = this.size.minDimension / 2 - strokePx / 2
      drawCircle(trackColor, radiusPx, style = Stroke(width = TrackStroke.toPx()))
      drawArc(
        color = arcColor,
        startAngle = -90f,
        sweepAngle = 360f * progress().coerceIn(0f, 1f),
        useCenter = false,
        topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
        size = Size(radiusPx * 2, radiusPx * 2),
        style = Stroke(width = strokePx, cap = StrokeCap.Round),
      )
    }
    Box(Modifier.fillMaxSize().padding(ContentInset), contentAlignment = Alignment.Center) {
      content()
    }
  }
}
