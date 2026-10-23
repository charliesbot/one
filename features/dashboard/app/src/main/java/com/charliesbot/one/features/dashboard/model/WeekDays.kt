package com.charliesbot.one.features.dashboard.model

import com.charliesbot.shared.core.models.FastingRules
import com.charliesbot.shared.core.models.TimePeriodProgress
import java.time.DayOfWeek
import java.util.Calendar

/** How a day looks in the week strip. */
internal sealed interface WeekDayMark {
  /** The day has a completed fast: drawn as the goal-reached shape. */
  data object Done : WeekDayMark

  /** A ring filled to [fraction]: a short fast in the past, or today. */
  data class Progress(val fraction: Float) : WeekDayMark

  /** A day that hasn't happened yet, so it can't have data. */
  data object Upcoming : WeekDayMark
}

internal data class WeekDay(
  val dayOfWeek: DayOfWeek,
  val isToday: Boolean,
  val mark: WeekDayMark,
  val fastingHours: Float,
)

/**
 * Builds Monday through Sunday from the week's history. Past days without a completed fast show
 * progress toward the minimum completed fast; today follows [activeFastProgress] while fasting.
 */
internal fun buildWeekDays(
  weekProgress: List<TimePeriodProgress>,
  today: DayOfWeek,
  activeFastProgress: Float?,
): List<WeekDay> {
  val progressByDay = weekProgress.associateBy { calendarDayToDayOfWeek(it.period) }
  return DayOfWeek.entries.map { day ->
    val record = progressByDay[day]
    val hours = record?.totalFastingHours ?: 0f
    val mark =
      when {
        day > today -> WeekDayMark.Upcoming
        record != null && record.progress >= 1f -> WeekDayMark.Done
        day == today && activeFastProgress != null -> WeekDayMark.Progress(activeFastProgress)
        else ->
          WeekDayMark.Progress((hours / FastingRules.MINIMUM_COMPLETED_FAST_HOURS).coerceIn(0f, 1f))
      }
    WeekDay(day, isToday = day == today, mark = mark, fastingHours = hours)
  }
}

// Calendar counts Sunday as 1 and Saturday as 7; DayOfWeek counts Monday as 1 and Sunday as 7.
private fun calendarDayToDayOfWeek(calendarDay: Int): DayOfWeek =
  if (calendarDay == Calendar.SUNDAY) DayOfWeek.SUNDAY else DayOfWeek.of(calendarDay - 1)
