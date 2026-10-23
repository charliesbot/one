package com.charliesbot.one.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.charliesbot.shared.core.designsystem.common.R as DesignSystemR
import kotlin.math.roundToInt

/** One end of the fast window: a small day name over the time. */
internal data class WindowEdge(val day: String, val time: String)

/** The idle card's fast window, from start to goal, with bedtime when it falls inside. */
internal data class FastWindowTimelineState(
  val start: WindowEdge,
  val end: WindowEdge,
  val bedtimeFraction: Float?,
  val bedtimeLabel: String?,
  /** What a screen reader says for the whole timeline. */
  val description: String,
)

private val TrackHeight = 16.dp
private val MarkerSize = 8.dp

/**
 * The idle card's fast window: one solid bar from start to goal with bedtime marked on it, so the
 * user sees how much of the fast happens while asleep. The bedtime label sits above the bar, where
 * nothing else competes for room, and the start and end times sit below it.
 */
@Composable
internal fun FastWindowTimeline(state: FastWindowTimelineState, modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colorScheme
  val bedtimeFraction = state.bedtimeFraction
  val bedtimeLabel = state.bedtimeLabel
  Column(
    modifier =
      modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = state.description },
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    if (bedtimeFraction != null && bedtimeLabel != null) {
      BedtimeAboveMarker(bedtimeLabel, bedtimeFraction)
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(TrackHeight)) {
      // The card's own pair: onPrimaryContainer on primaryContainer, with the marker cut back out.
      Box(
        Modifier.fillMaxWidth()
          .height(TrackHeight)
          .background(colors.onPrimaryContainer, CircleShape)
      )
      bedtimeFraction?.let { fraction ->
        BedtimeMarker(
          color = colors.primaryContainer,
          x = maxWidth * fraction,
          modifier = Modifier.align(Alignment.CenterStart),
        )
      }
    }
    Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
      EdgeLabel(state.start, TextAlign.Start, Modifier.align(Alignment.TopStart))
      EdgeLabel(state.end, TextAlign.End, Modifier.align(Alignment.TopEnd))
    }
  }
}

/** The bedtime label centered over its marker, kept inside the bar's width. */
@Composable
private fun BedtimeAboveMarker(time: String, fraction: Float) {
  Layout(content = { BedtimeLabel(time) }, modifier = Modifier.fillMaxWidth()) {
    measurables,
    constraints ->
    val label = measurables.single().measure(constraints.copy(minWidth = 0))
    val width = constraints.maxWidth
    val x = bedtimeLabelX((width * fraction).roundToInt(), label.width, width)
    // Relative placement mirrors in right-to-left layouts, like the marker's start alignment.
    layout(width, label.height) { label.placeRelative(x, 0) }
  }
}

/** Where the bedtime label starts: centered on its marker, but never past either end of the bar. */
internal fun bedtimeLabelX(markerX: Int, labelWidth: Int, totalWidth: Int): Int =
  (markerX - labelWidth / 2).coerceIn(0, (totalWidth - labelWidth).coerceAtLeast(0))

@Composable
private fun BedtimeLabel(time: String) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = painterResource(DesignSystemR.drawable.bedtime_filled_24px),
      contentDescription = null,
      modifier = Modifier.size(18.dp),
    )
    Text(text = time, style = MaterialTheme.typography.labelMediumEmphasized)
  }
}

/**
 * The active fast window: filled up to now, a slider-style handle at now, the rest as track, and
 * bedtime marked inside the filled part once it has passed.
 */
@Composable
internal fun FastWindowProgressBar(
  progress: Float,
  bedtimeFraction: Float?,
  start: String,
  end: String,
  description: String,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val handleGap = 6.dp
  Column(
    modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(32.dp)) {
      val handleX = maxWidth * progress
      val fillWidth = (handleX - handleGap).coerceAtLeast(0.dp)
      val restStart = handleX + handleGap
      Box(
        Modifier.align(Alignment.CenterStart)
          .width(fillWidth)
          .height(TrackHeight)
          .background(
            colors.primary,
            RoundedCornerShape(
              topStart = TrackHeight / 2,
              bottomStart = TrackHeight / 2,
              topEnd = 4.dp,
              bottomEnd = 4.dp,
            ),
          )
      )
      if (restStart < maxWidth) {
        Box(
          Modifier.align(Alignment.CenterStart)
            .offset(x = restStart)
            .width(maxWidth - restStart)
            .height(TrackHeight)
            .background(
              colors.surfaceContainerHighest,
              RoundedCornerShape(
                topStart = 4.dp,
                bottomStart = 4.dp,
                topEnd = TrackHeight / 2,
                bottomEnd = TrackHeight / 2,
              ),
            )
        )
      }
      Box(
        Modifier.align(Alignment.CenterStart)
          .offset(x = handleX - 2.dp)
          .width(4.dp)
          .height(32.dp)
          .background(colors.primary, CircleShape)
      )
      bedtimeFraction
        ?.takeIf { it < progress }
        ?.let { fraction ->
          BedtimeMarker(
            color = colors.onPrimary,
            x = maxWidth * fraction,
            modifier = Modifier.align(Alignment.CenterStart),
          )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(start, style = MaterialTheme.typography.titleSmallEmphasized)
      Text(end, style = MaterialTheme.typography.titleSmallEmphasized)
    }
  }
}

@Composable
private fun BedtimeMarker(color: Color, x: Dp, modifier: Modifier) {
  Box(modifier.offset(x = x - MarkerSize / 2).size(MarkerSize).background(color, CircleShape))
}

@Composable
private fun EdgeLabel(edge: WindowEdge, align: TextAlign, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier,
    horizontalAlignment = if (align == TextAlign.End) Alignment.End else Alignment.Start,
  ) {
    Text(edge.day, style = MaterialTheme.typography.labelMedium)
    Text(edge.time, style = MaterialTheme.typography.titleMediumEmphasized)
  }
}
