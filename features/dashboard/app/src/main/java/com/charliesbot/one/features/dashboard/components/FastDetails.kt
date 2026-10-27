package com.charliesbot.one.features.dashboard.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.charliesbot.one.features.dashboard.model.ClockText
import com.charliesbot.shared.core.designsystem.common.R as DesignSystemR

/** The editable facts of the fast in progress. */
internal data class FastDetailsState(
  /** "Started", or "Started Wed" when the fast began on an earlier day. */
  val startedLabel: String,
  val startedClock: ClockText,
  /** "16h goal", or "16h goal · Fri" when the goal falls on another day. */
  val goalLabel: String,
  val goalClock: ClockText,
)

/**
 * Started and Goal as two tappable tiles side by side, each in its own tint with a library-shape
 * badge, a small label and the time. Tapping a tile opens its editor. [stacked] puts the badge
 * above the text, for the narrow tiles beside the dial on large screens.
 */
@Composable
internal fun FastDetails(
  state: FastDetailsState,
  onEditStart: () -> Unit,
  onEditGoal: () -> Unit,
  stacked: Boolean,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    DetailTile(
      badgeShape = StartedShape,
      badgeIcon = DesignSystemR.drawable.schedule_24px,
      containerColor = colors.secondaryContainer,
      contentColor = colors.onSecondaryContainer,
      badgeContentColor = colors.secondary,
      label = state.startedLabel,
      clock = state.startedClock,
      onClick = onEditStart,
      stacked = stacked,
      modifier = Modifier.weight(1f),
    )
    DetailTile(
      badgeShape = GoalReachedShape,
      badgeIcon = DesignSystemR.drawable.flag_24px,
      containerColor = colors.tertiaryContainer,
      contentColor = colors.onTertiaryContainer,
      badgeContentColor = colors.tertiary,
      label = state.goalLabel,
      clock = state.goalClock,
      onClick = onEditGoal,
      stacked = stacked,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun DetailTile(
  badgeShape: RoundedPolygon,
  @DrawableRes badgeIcon: Int,
  containerColor: Color,
  contentColor: Color,
  badgeContentColor: Color,
  label: String,
  clock: ClockText,
  onClick: () -> Unit,
  stacked: Boolean,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
  ) {
    if (stacked) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        TileBadge(badgeShape, badgeIcon, badgeContentColor)
        DetailTileText(label, clock)
      }
    } else {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TileBadge(badgeShape, badgeIcon, badgeContentColor)
        DetailTileText(label, clock, Modifier.weight(1f))
      }
    }
  }
}

@Composable
private fun TileBadge(shape: RoundedPolygon, @DrawableRes icon: Int, iconColor: Color) {
  // A light badge with the icon in the tile's accent (a soft grey in dark mode).
  ShapeBadge(shape, icon, MaterialTheme.colorScheme.surfaceBright, iconColor)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DetailTileText(label: String, clock: ClockText, modifier: Modifier = Modifier) {
  Column(modifier) {
    Text(
      label,
      style = MaterialTheme.typography.labelLarge,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    // At large font scales the time shrinks to fit instead of wrapping.
    Row(Modifier.scaleToFitWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        clock.time,
        style = TodayTextStyles.TileValue,
        maxLines = 1,
        modifier = Modifier.alignByBaseline(),
      )
      clock.period?.let {
        Text(
          it,
          style = MaterialTheme.typography.labelLargeEmphasized,
          maxLines = 1,
          modifier = Modifier.alignByBaseline(),
        )
      }
    }
  }
}
