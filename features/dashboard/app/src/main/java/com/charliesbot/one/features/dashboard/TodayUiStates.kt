package com.charliesbot.one.features.dashboard

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.charliesbot.one.features.dashboard.components.ActiveHeroState
import com.charliesbot.one.features.dashboard.components.FastDetailsState
import com.charliesbot.one.features.dashboard.components.FastWindowCardState
import com.charliesbot.one.features.dashboard.components.FastWindowTimelineState
import com.charliesbot.one.features.dashboard.components.GoalOption
import com.charliesbot.one.features.dashboard.components.IdleHeroState
import com.charliesbot.one.features.dashboard.components.WindowEdge
import com.charliesbot.one.features.dashboard.model.ClockText
import com.charliesbot.one.features.dashboard.model.FastWindow
import com.charliesbot.one.features.dashboard.model.HoursMinutes
import com.charliesbot.one.features.dashboard.model.suggestedStartMillis
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.designsystem.common.R as DesignSystemR
import com.charliesbot.shared.core.models.SuggestedFastingTime
import com.charliesbot.shared.core.models.SuggestionSource
import com.charliesbot.shared.core.strings.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** Locale, clock style and zone for turning times into the Today screen's text. */
internal data class TimeFormatting(val locale: Locale, val is24Hour: Boolean, val zone: ZoneId) {
  fun dateTime(millis: Long): ZonedDateTime = Instant.ofEpochMilli(millis).atZone(zone)

  fun clock(millis: Long): ClockText =
    ClockText.of(dateTime(millis).toLocalTime(), is24Hour, locale)

  fun compactClock(millis: Long): String =
    ClockText.compact(dateTime(millis).toLocalTime(), is24Hour, locale)

  fun shortDay(millis: Long): String =
    dateTime(millis).dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
}

/** "Thursday, October 23" in the user's locale. */
internal fun headerDate(nowMillis: Long, format: TimeFormatting): String {
  val pattern = DateFormat.getBestDateTimePattern(format.locale, "EEEEMMMMd")
  return format.dateTime(nowMillis).format(DateTimeFormatter.ofPattern(pattern, format.locale))
}

@Composable
@ReadOnlyComposable
internal fun currentTimeFormatting(): TimeFormatting =
  TimeFormatting(
    locale = LocalConfiguration.current.locales[0],
    is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    zone = ZoneId.systemDefault(),
  )

/** "6h 18m", "4h" or "12m". [roundUp] counts a partial minute as whole, for time left. */
@Composable
internal fun durationText(millis: Long, roundUp: Boolean = false): String {
  val (hours, minutes) = if (roundUp) HoursMinutes.roundedUp(millis) else HoursMinutes.of(millis)
  return when {
    hours == 0L -> stringResource(R.string.duration_minutes, minutes)
    minutes == 0L -> stringResource(R.string.duration_hours, hours)
    else -> stringResource(R.string.duration_hours_minutes, hours, minutes)
  }
}

/** The first line of a goal's title, such as "16:8" or "Circadian". */
@Composable
internal fun goalShortName(goal: FastGoal): String =
  (goal.titleText ?: stringResource(goal.titleResId)).lineSequence().first().trim()

/**
 * The idle card. With a suggestion the window starts at the suggested time; without one it starts
 * now, and the big time becomes when that fast would finish.
 */
@Composable
internal fun idleHeroState(
  goal: FastGoal,
  suggestion: SuggestedFastingTime?,
  bedtimeMinutes: Int?,
  nowMillis: Long,
  format: TimeFormatting,
): IdleHeroState {
  if (suggestion == null) {
    val window = FastWindow.create(nowMillis, goal.durationMillis, bedtimeMinutes, format.zone)
    return IdleHeroState(
      eyebrow = stringResource(R.string.start_now_finish_at),
      eyebrowIcon = DesignSystemR.drawable.flag_24px,
      clock = format.clock(window.endMillis),
      detail =
        stringResource(
          R.string.finish_from_now,
          relativeDay(window.endMillis, nowMillis, format),
          durationText(goal.durationMillis),
        ),
      window = fastWindowTimelineState(window, stringResource(R.string.starts_now), format),
      goalLabel = goalShortName(goal),
    )
  }
  // The suggestion's timestamp is computed once and goes stale; its time of day does not.
  val startMillis = suggestedStartMillis(suggestion.suggestedTimeMinutes, nowMillis, format.zone)
  val window = FastWindow.create(startMillis, goal.durationMillis, bedtimeMinutes, format.zone)
  val untilStart = window.startMillis - nowMillis
  val countdown =
    if (untilStart < 60_000L) stringResource(R.string.starts_now)
    else stringResource(R.string.starts_in, durationText(untilStart))
  return IdleHeroState(
    eyebrow = stringResource(R.string.next_fast),
    eyebrowIcon = DesignSystemR.drawable.bedtime_24px,
    clock = format.clock(window.startMillis),
    detail = suggestionDetail(suggestion.source, countdown, window, format),
    window = fastWindowTimelineState(window, format.compactClock(window.startMillis), format),
    goalLabel = goalShortName(goal),
  )
}

/** Why the suggested time was picked, after the countdown to it. */
@Composable
private fun suggestionDetail(
  source: SuggestionSource,
  countdown: String,
  window: FastWindow,
  format: TimeFormatting,
): String =
  when (source) {
    SuggestionSource.BEDTIME_BASED ->
      window.bedtimeMillis?.let { bedtime ->
        stringResource(
          R.string.next_fast_reason_bedtime,
          countdown,
          durationText(bedtime - window.startMillis),
          format.compactClock(bedtime),
        )
      } ?: countdown
    SuggestionSource.MOVING_AVERAGE -> stringResource(R.string.next_fast_reason_average, countdown)
    SuggestionSource.FIXED_TIME -> stringResource(R.string.next_fast_reason_fixed, countdown)
  }

/** The idle timeline for [window]; [startTime] is "Now" or the start's clock time. */
private fun fastWindowTimelineState(
  window: FastWindow,
  startTime: String,
  format: TimeFormatting,
): FastWindowTimelineState {
  val start = WindowEdge(format.shortDay(window.startMillis), startTime)
  val end = WindowEdge(format.shortDay(window.endMillis), format.compactClock(window.endMillis))
  return FastWindowTimelineState(
    start = start,
    end = end,
    bedtimeFraction = window.bedtimeFraction,
    bedtimeLabel = window.bedtimeMillis?.let { format.compactClock(it) },
    description = "${start.day} ${start.time} – ${end.day} ${end.time}",
  )
}

@Composable
private fun relativeDay(millis: Long, nowMillis: Long, format: TimeFormatting): String {
  val day = format.dateTime(millis).toLocalDate()
  val today: LocalDate = format.dateTime(nowMillis).toLocalDate()
  return when (day) {
    today -> stringResource(R.string.day_today)
    today.plusDays(1) -> stringResource(R.string.day_tomorrow)
    else -> day.dayOfWeek.getDisplayName(TextStyle.FULL, format.locale)
  }
}

@Composable
internal fun activeHeroState(window: FastWindow, nowMillis: Long): ActiveHeroState {
  val elapsedSeconds = ((nowMillis - window.startMillis).coerceAtLeast(0L)) / 1000
  val hours = elapsedSeconds / 3600
  val minutes = (elapsedSeconds % 3600) / 60
  val seconds = elapsedSeconds % 60
  return ActiveHeroState(
    progress = window.progressAt(nowMillis),
    elapsed = "%d:%02d".format(hours, minutes),
    elapsedSeconds = ":%02d".format(seconds),
    goalStatus = goalStatus(window, nowMillis),
  )
}

/** "6h 18m to go" before the goal, then "Goal reached", then "Goal reached · 4h over". */
@Composable
private fun goalStatus(window: FastWindow, nowMillis: Long): String {
  val overtime = window.overtimeAt(nowMillis)
  return when {
    window.remainingAt(nowMillis) > 0L ->
      stringResource(
        R.string.time_to_go,
        durationText(window.remainingAt(nowMillis), roundUp = true),
      )
    overtime < 60_000L -> stringResource(R.string.goal_reached)
    else -> stringResource(R.string.goal_reached_over, durationText(overtime))
  }
}

@Composable
internal fun fastDetailsState(
  window: FastWindow,
  goal: FastGoal,
  nowMillis: Long,
  format: TimeFormatting,
): FastDetailsState =
  FastDetailsState(
    // The day only matters when the fast began on an earlier day.
    startedLabel =
      if (
        format.dateTime(window.startMillis).toLocalDate() ==
          format.dateTime(nowMillis).toLocalDate()
      ) {
        stringResource(R.string.started)
      } else {
        stringResource(R.string.started_on_day, format.shortDay(window.startMillis))
      },
    startedClock = format.clock(window.startMillis),
    goalLabel = stringResource(R.string.goal_hours_label, goal.durationDisplay),
    goalClock = format.clock(window.endMillis),
  )

@Composable
internal fun goalOptions(goals: List<FastGoal>): List<GoalOption> =
  goals.map { goal ->
    GoalOption(
      id = goal.id,
      label =
        "${goalShortName(goal)} · ${stringResource(R.string.duration_hours, TimeUnit.MILLISECONDS.toHours(goal.durationMillis))}",
    )
  }

/** The large-screen fast window card for the fast in progress. */
@Composable
internal fun fastWindowCardState(
  window: FastWindow,
  nowMillis: Long,
  format: TimeFormatting,
): FastWindowCardState {
  val progress = window.progressAt(nowMillis)
  val start = "${format.shortDay(window.startMillis)}, ${format.compactClock(window.startMillis)}"
  val end = "${format.shortDay(window.endMillis)}, ${format.compactClock(window.endMillis)}"
  return FastWindowCardState(
    progress = progress,
    bedtimeFraction = window.bedtimeFraction,
    start = start,
    end = end,
    bedtimeNote = bedtimeNote(window, nowMillis, format),
    description =
      stringResource(R.string.fast_window_description, (progress * 100).roundToInt(), start, end),
  )
}

@Composable
private fun bedtimeNote(window: FastWindow, nowMillis: Long, format: TimeFormatting): String? {
  val bedtime = window.bedtimeMillis ?: return null
  val time = format.compactClock(bedtime)
  return if (bedtime <= nowMillis) stringResource(R.string.asleep_since, time)
  else stringResource(R.string.bedtime_at, time)
}
