package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.data.db.FastingRecord
import com.charliesbot.shared.core.domain.repository.FastingHistoryRepository
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetMonthlyFastingMapUseCaseTest {

  private lateinit var repository: FastingHistoryRepository
  private lateinit var useCase: GetMonthlyFastingMapUseCase

  private val january2025 = YearMonth.of(2025, 1)

  @Before
  fun setup() {
    repository = mockk()
    useCase = GetMonthlyFastingMapUseCase(repository)
  }

  @Test
  fun `empty month returns empty map`() = runTest {
    every { repository.getFastingsForMonth(january2025) } returns flowOf(emptyList())

    val result = useCase(january2025).first()

    assertTrue(result.isEmpty())
  }

  @Test
  fun `picks longest fast when multiple fasts end on same day`() = runTest {
    val endDate = LocalDate.of(2025, 1, 15)
    val endMillis =
      endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 12 * 3_600_000

    val shortFast =
      FastingRecord(
        startTimeEpochMillis = endMillis - 10 * 3_600_000L, // 10h
        endTimeEpochMillis = endMillis,
        fastingGoalId = "circadian",
      )
    val longFast =
      FastingRecord(
        startTimeEpochMillis = endMillis - 16 * 3_600_000L, // 16h
        endTimeEpochMillis = endMillis,
        fastingGoalId = "16:8",
      )

    every { repository.getFastingsForMonth(january2025) } returns
      flowOf(listOf(shortFast, longFast))

    val result = useCase(january2025).first()

    assertEquals(16, result[endDate]?.durationHours)
    assertEquals("16:8", result[endDate]?.goalId)
  }

  @Test
  fun `13h fast is marked as goal met`() = runTest {
    val endDate = LocalDate.of(2025, 1, 10)
    val endMillis =
      endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 18 * 3_600_000
    val startMillis = endMillis - 13 * 3_600_000L

    every { repository.getFastingsForMonth(january2025) } returns
      flowOf(
        listOf(
          FastingRecord(
            startTimeEpochMillis = startMillis,
            endTimeEpochMillis = endMillis,
            fastingGoalId = "circadian",
          )
        )
      )

    val result = useCase(january2025).first()

    assertTrue(result[endDate]!!.isGoalMet)
  }

  @Test
  fun `12h fast is marked as goal not met`() = runTest {
    val endDate = LocalDate.of(2025, 1, 10)
    val endMillis =
      endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 18 * 3_600_000
    val startMillis = endMillis - 12 * 3_600_000L

    every { repository.getFastingsForMonth(january2025) } returns
      flowOf(
        listOf(
          FastingRecord(
            startTimeEpochMillis = startMillis,
            endTimeEpochMillis = endMillis,
            fastingGoalId = "circadian",
          )
        )
      )

    val result = useCase(january2025).first()

    assertFalse(result[endDate]!!.isGoalMet)
  }

  @Test
  fun `multiple days are mapped correctly`() = runTest {
    val day1End =
      LocalDate.of(2025, 1, 5).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() +
        12 * 3_600_000
    val day2End =
      LocalDate.of(2025, 1, 6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() +
        12 * 3_600_000

    val records =
      listOf(
        FastingRecord(
          startTimeEpochMillis = day1End - 16 * 3_600_000L,
          endTimeEpochMillis = day1End,
          fastingGoalId = "16:8",
        ),
        FastingRecord(
          startTimeEpochMillis = day2End - 18 * 3_600_000L,
          endTimeEpochMillis = day2End,
          fastingGoalId = "18:6",
        ),
      )

    every { repository.getFastingsForMonth(january2025) } returns flowOf(records)

    val result = useCase(january2025).first()

    assertEquals(2, result.size)
    assertEquals(16, result[LocalDate.of(2025, 1, 5)]?.durationHours)
    assertEquals(18, result[LocalDate.of(2025, 1, 6)]?.durationHours)
  }

  @Test
  fun `groups by end date not start date for overnight fasts`() = runTest {
    // Fast starts Jan 9 at 20:00, ends Jan 10 at 12:00 (16h)
    val startDate = LocalDate.of(2025, 1, 9)
    val endDate = LocalDate.of(2025, 1, 10)
    val startMillis =
      startDate.atTime(20, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endMillis = endDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    every { repository.getFastingsForMonth(january2025) } returns
      flowOf(
        listOf(
          FastingRecord(
            startTimeEpochMillis = startMillis,
            endTimeEpochMillis = endMillis,
            fastingGoalId = "16:8",
          )
        )
      )

    val result = useCase(january2025).first()

    // Should be keyed by end date (Jan 10), not start date (Jan 9)
    assertTrue(result.containsKey(endDate))
    assertFalse(result.containsKey(startDate))
  }

  @Test
  fun `all FastingDayData fields populated correctly`() = runTest {
    val endDate = LocalDate.of(2025, 1, 20)
    val startMillis =
      endDate.atTime(0, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - 4 * 3_600_000
    val endMillis = endDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    every { repository.getFastingsForMonth(january2025) } returns
      flowOf(
        listOf(
          FastingRecord(
            startTimeEpochMillis = startMillis,
            endTimeEpochMillis = endMillis,
            fastingGoalId = "18:6",
          )
        )
      )

    val result = useCase(january2025).first()
    val dayData = result[endDate]!!

    assertEquals(endDate, dayData.date)
    assertEquals(16, dayData.durationHours) // 16h duration
    assertTrue(dayData.isGoalMet) // 16 >= 13
    assertEquals(startMillis, dayData.startTimeEpochMillis)
    assertEquals(endMillis, dayData.endTimeEpochMillis)
    assertEquals("18:6", dayData.goalId)
  }
}
