package com.charliesbot.one.features.dashboard.model

import com.charliesbot.shared.core.models.TimePeriodProgress
import com.charliesbot.shared.core.models.TimePeriodType
import java.time.DayOfWeek
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekDaysTest {

  private val calendarDays =
    listOf(
      Calendar.MONDAY,
      Calendar.TUESDAY,
      Calendar.WEDNESDAY,
      Calendar.THURSDAY,
      Calendar.FRIDAY,
      Calendar.SATURDAY,
      Calendar.SUNDAY,
    )

  private fun week(vararg hours: Float): List<TimePeriodProgress> =
    calendarDays.mapIndexed { index, day ->
      val dayHours = hours.getOrElse(index) { 0f }
      TimePeriodProgress(
        period = day,
        periodType = TimePeriodType.DAY_OF_WEEK,
        progress = if (dayHours >= 13f) 1f else 0f,
        totalFastingHours = dayHours,
      )
    }

  @Test
  fun `days map to Monday through Sunday`() {
    val days = buildWeekDays(week(), today = DayOfWeek.THURSDAY, activeFastProgress = null)

    assertEquals(DayOfWeek.entries.toList(), days.map { it.dayOfWeek })
  }

  @Test
  fun `completed days are done and short fasts show progress toward the minimum`() {
    val days =
      buildWeekDays(week(16f, 17f, 6.5f), today = DayOfWeek.THURSDAY, activeFastProgress = null)

    assertEquals(WeekDayMark.Done, days[0].mark)
    assertEquals(WeekDayMark.Done, days[1].mark)
    assertEquals(WeekDayMark.Progress(0.5f), days[2].mark)
  }

  @Test
  fun `days after today are upcoming`() {
    val days = buildWeekDays(week(), today = DayOfWeek.THURSDAY, activeFastProgress = null)

    assertEquals(
      listOf(WeekDayMark.Upcoming, WeekDayMark.Upcoming, WeekDayMark.Upcoming),
      days.drop(4).map { it.mark },
    )
  }

  @Test
  fun `today without a fast is an empty ring`() {
    val days = buildWeekDays(week(), today = DayOfWeek.THURSDAY, activeFastProgress = null)

    assertEquals(WeekDayMark.Progress(0f), days[3].mark)
    assertEquals(listOf(DayOfWeek.THURSDAY), days.filter { it.isToday }.map { it.dayOfWeek })
  }

  @Test
  fun `today follows the fast in progress`() {
    val days = buildWeekDays(week(), today = DayOfWeek.THURSDAY, activeFastProgress = 0.61f)

    assertEquals(WeekDayMark.Progress(0.61f), days[3].mark)
  }

  @Test
  fun `today stays done once a fast was completed`() {
    val days =
      buildWeekDays(week(0f, 0f, 0f, 14f), today = DayOfWeek.THURSDAY, activeFastProgress = 0.2f)

    assertEquals(WeekDayMark.Done, days[3].mark)
  }

  @Test
  fun `missing history still yields a full week`() {
    val days = buildWeekDays(emptyList(), today = DayOfWeek.MONDAY, activeFastProgress = null)

    assertEquals(7, days.size)
    assertEquals(WeekDayMark.Progress(0f), days[0].mark)
    assertEquals(WeekDayMark.Upcoming, days[6].mark)
  }
}
