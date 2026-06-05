package com.charliesbot.shared.core.domain.progress

import com.charliesbot.shared.core.models.FastingRecord
import com.charliesbot.shared.core.models.TimePeriodType
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class FastingProgressCalculatorTest {

  @Test
  fun `weekly progress returns monday through sunday entries`() {
    val progress = FastingProgressCalculator.calculateWeeklyProgress(emptyList())

    assertEquals(
      listOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY,
      ),
      progress.map { it.period },
    )
    assertEquals(List(7) { TimePeriodType.DAY_OF_WEEK }, progress.map { it.periodType })
  }

  @Test
  fun `weekly progress marks day complete when longest fast reaches minimum hours`() {
    val mondayRecord =
      FastingRecord(
        startTimeEpochMillis = millisFor(2026, Calendar.JUNE, 1, 0),
        endTimeEpochMillis = millisFor(2026, Calendar.JUNE, 1, 13),
        fastingGoalId = "16:8",
      )

    val progress = FastingProgressCalculator.calculateWeeklyProgress(listOf(mondayRecord))
    val mondayProgress = progress.first { it.period == Calendar.MONDAY }

    assertEquals(1.0f, mondayProgress.progress)
    assertEquals(1, mondayProgress.completedFasts)
    assertEquals(13f, mondayProgress.totalFastingHours)
  }

  @Test
  fun `weekly progress marks day incomplete when longest fast is below minimum hours`() {
    val tuesdayRecord =
      FastingRecord(
        startTimeEpochMillis = millisFor(2026, Calendar.JUNE, 2, 0),
        endTimeEpochMillis = millisFor(2026, Calendar.JUNE, 2, 12),
        fastingGoalId = "16:8",
      )

    val progress = FastingProgressCalculator.calculateWeeklyProgress(listOf(tuesdayRecord))
    val tuesdayProgress = progress.first { it.period == Calendar.TUESDAY }

    assertEquals(0.0f, tuesdayProgress.progress)
    assertEquals(1, tuesdayProgress.completedFasts)
    assertEquals(12f, tuesdayProgress.totalFastingHours)
  }

  private fun millisFor(year: Int, month: Int, day: Int, hour: Int): Long =
    Calendar.getInstance()
      .apply {
        clear()
        set(year, month, day, hour, 0, 0)
      }
      .timeInMillis
}
