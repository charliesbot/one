package com.charliesbot.one.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.charliesbot.one.features.dashboard.model.WeekDay
import com.charliesbot.one.features.dashboard.model.WeekDayMark
import com.charliesbot.shared.core.designsystem.common.R as DesignSystemR
import com.charliesbot.shared.core.designsystem.common.components.FastingProgressBar
import com.charliesbot.shared.core.strings.R
import java.time.DayOfWeek
import java.time.format.TextStyle as DayTextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val MarkSize = 32.dp

/**
 * Monday to Sunday: completed days as the goal-reached shape, past and current days as rings,
 * upcoming days as small dots. Today sits in a tonal pill. [showHours] adds each day's longest fast
 * under its mark, for large screens.
 */
@Composable
internal fun WeekStrip(days: List<WeekDay>, showHours: Boolean, modifier: Modifier = Modifier) {
  Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    days.forEach { day -> WeekStripDay(day, showHours, Modifier.weight(1f)) }
  }
}

@Composable
private fun WeekStripDay(day: WeekDay, showHours: Boolean, modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colorScheme
  val name = day.dayOfWeek.getDisplayName(DayTextStyle.SHORT, Locale.getDefault())
  val description =
    when (val mark = day.mark) {
      WeekDayMark.Done -> stringResource(R.string.week_day_done, name)
      WeekDayMark.Upcoming -> stringResource(R.string.week_day_upcoming, name)
      is WeekDayMark.Progress ->
        stringResource(R.string.week_day_progress, name, (mark.fraction * 100).roundToInt())
    }
  val labelColor = if (day.isToday) colors.onSecondaryContainer else colors.onSurfaceVariant
  val labelStyle =
    if (day.isToday) MaterialTheme.typography.labelMediumEmphasized
    else MaterialTheme.typography.labelMedium

  Column(
    modifier =
      modifier
        .then(
          if (day.isToday) {
            Modifier.background(colors.secondaryContainer, RoundedCornerShape(28.dp))
          } else {
            Modifier
          }
        )
        .padding(top = 8.dp, bottom = 12.dp)
        .clearAndSetSemantics { contentDescription = description },
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(text = name, style = labelStyle, color = labelColor)
    DayMark(day.mark, isToday = day.isToday)
    if (showHours) {
      val hoursText =
        when {
          day.isToday && day.mark !is WeekDayMark.Done && day.fastingHours < 1f ->
            stringResource(R.string.day_today)
          day.fastingHours > 0f -> stringResource(R.string.duration_hours, day.fastingHours.toInt())
          else -> "–"
        }
      Text(text = hoursText, style = labelStyle, color = labelColor)
    }
  }
}

@Composable
private fun DayMark(mark: WeekDayMark, isToday: Boolean) {
  val colors = MaterialTheme.colorScheme
  when (mark) {
    // A reached goal is status, so tertiary, matching the Goal tile; primary stays on the actions.
    WeekDayMark.Done ->
      ShapeBadge(
        shape = GoalReachedShape,
        icon = DesignSystemR.drawable.check_24px,
        containerColor = colors.tertiary,
        contentColor = colors.onTertiary,
        size = MarkSize,
      )
    is WeekDayMark.Progress ->
      FastingProgressBar(
        progress = mark.fraction,
        strokeWidth = 4.dp,
        indicatorColor = colors.primary,
        // Today's empty track is tinted so it shows on the pill yet still reads as empty.
        trackColor = if (isToday) colors.primaryContainer else colors.surfaceContainerHighest,
        modifier = Modifier.size(MarkSize),
      )
    WeekDayMark.Upcoming ->
      Box(modifier = Modifier.size(MarkSize), contentAlignment = Alignment.Center) {
        Box(Modifier.size(6.dp).background(colors.outlineVariant, CircleShape))
      }
  }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun WeekStripPreview() {
  val days =
    DayOfWeek.entries.mapIndexed { index, day ->
      val mark =
        when (index) {
          0,
          1 -> WeekDayMark.Done
          2 -> WeekDayMark.Progress(0.5f)
          3 -> WeekDayMark.Progress(0f)
          else -> WeekDayMark.Upcoming
        }
      val hours = listOf(16f, 17f, 6f).getOrElse(index) { 0f }
      WeekDay(day, isToday = index == 3, mark = mark, fastingHours = hours)
    }
  MaterialTheme { WeekStrip(days, showHours = true) }
}
