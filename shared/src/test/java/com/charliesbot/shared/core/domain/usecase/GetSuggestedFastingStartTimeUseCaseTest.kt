package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.data.db.FastingRecord
import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepository
import com.charliesbot.shared.core.data.repositories.settingsRepository.SettingsRepository
import com.charliesbot.shared.core.data.repositories.settingsRepository.SmartReminderMode
import com.charliesbot.shared.core.models.SuggestionSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class GetSuggestedFastingStartTimeUseCaseTest {

    private lateinit var historyRepository: FastingHistoryRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: GetSuggestedFastingStartTimeUseCase

    @Before
    fun setup() {
        // Mock Android Log to prevent failures in unit tests
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0

        historyRepository = mockk()
        settingsRepository = mockk()
        useCase = GetSuggestedFastingStartTimeUseCase(historyRepository, settingsRepository)
    }

    // ============= BEDTIME_ONLY Mode Tests =============

    @Test
    fun `execute with BEDTIME_ONLY mode returns bedtime minus 4 hours`() = runTest {
        // Arrange: Bedtime at 22:00 (1320 minutes) -> should suggest 18:00 (1080 minutes)
        val bedtimeMinutes = 1320 // 22:00
        every { settingsRepository.smartReminderMode } returns flowOf(SmartReminderMode.BEDTIME_ONLY)
        every { settingsRepository.bedtimeMinutes } returns flowOf(bedtimeMinutes)
        every { historyRepository.getAllHistory() } returns flowOf(emptyList())

        // Act
        val result = useCase.execute()

        // Assert
        assertEquals(SuggestionSource.BEDTIME_BASED, result.source)
        assertEquals(1080, result.suggestedTimeMinutes) // 18:00
        assertTrue(result.reasoning.contains("22:00"))
        assertTrue(result.reasoning.contains("bedtime"))
    }

    @Test
    fun `execute with BEDTIME_ONLY mode handles midnight wrap-around`() = runTest {
        // Arrange: Bedtime at 01:00 (60 minutes) -> should suggest 21:00 (1260 minutes)
        val bedtimeMinutes = 60 // 01:00
        every { settingsRepository.smartReminderMode } returns flowOf(SmartReminderMode.BEDTIME_ONLY)
        every { settingsRepository.bedtimeMinutes } returns flowOf(bedtimeMinutes)
        every { historyRepository.getAllHistory() } returns flowOf(emptyList())

        // Act
        val result = useCase.execute()

        // Assert
        assertEquals(SuggestionSource.BEDTIME_BASED, result.source)
        assertEquals(1260, result.suggestedTimeMinutes) // 21:00 (wraps around)
    }

    // ============= FIXED_TIME Mode Tests =============

    @Test
    fun `execute with FIXED_TIME mode returns user configured time`() = runTest {
        // Arrange: Fixed time at 19:00 (1140 minutes)
        val fixedMinutes = 1140 // 19:00
        every { settingsRepository.smartReminderMode } returns flowOf(SmartReminderMode.FIXED_TIME)
        every { settingsRepository.fixedFastingStartMinutes } returns flowOf(fixedMinutes)
        every { historyRepository.getAllHistory() } returns flowOf(emptyList())

        // Act
        val result = useCase.execute()

        // Assert
        assertEquals(SuggestionSource.FIXED_TIME, result.source)
        assertEquals(1140, result.suggestedTimeMinutes) // 19:00
        assertEquals("Your scheduled fasting time", result.reasoning)
    }

    // ============= MOVING_AVERAGE_ONLY Mode Tests =============

    @Test
    fun `execute with MOVING_AVERAGE_ONLY mode and enough history uses average`() = runTest {
        // Arrange: 3 records at 19:00, 20:00, 21:00 -> average should be 20:00 (1200 minutes)
        val now = System.currentTimeMillis()
        val records = listOf(
            createFastingRecordAtTime(19, 0, now - 1 * 24 * 60 * 60 * 1000L), // 19:00 yesterday
            createFastingRecordAtTime(20, 0, now - 2 * 24 * 60 * 60 * 1000L), // 20:00 2 days ago
            createFastingRecordAtTime(21, 0, now - 3 * 24 * 60 * 60 * 1000L)  // 21:00 3 days ago
        )

        every { settingsRepository.smartReminderMode } returns flowOf(SmartReminderMode.MOVING_AVERAGE_ONLY)
        every { historyRepository.getAllHistory() } returns flowOf(records)

        // Act
        val result = useCase.execute()

        // Assert
        assertEquals(SuggestionSource.MOVING_AVERAGE, result.source)
        // Average of 19, 20, 21 hours = 20 hours = 1200 minutes
        assertEquals(1200, result.suggestedTimeMinutes)
        assertTrue(result.reasoning.contains("average"))
    }

    @Test
    fun `execute with MOVING_AVERAGE_ONLY mode and not enough history falls back to bedtime`() = runTest {
        // Arrange: Only 2 records (need 3 minimum)
        val now = System.currentTimeMillis()
        val records = listOf(
            createFastingRecordAtTime(19, 0, now - 1 * 24 * 60 * 60 * 1000L),
            createFastingRecordAtTime(20, 0, now - 2 * 24 * 60 * 60 * 1000L)
        )

        every { settingsRepository.smartReminderMode } returns flowOf(SmartReminderMode.MOVING_AVERAGE_ONLY)
        every { settingsRepository.bedtimeMinutes } returns flowOf(1320) // 22:00
        every { historyRepository.getAllHistory() } returns flowOf(records)

        // Act
        val result = useCase.execute()

        // Assert
        assertEquals(SuggestionSource.BEDTIME_BASED, result.source)
        assertTrue(result.reasoning.contains("Not enough history"))
    }

    // ============= AUTO Mode Tests =============

    @Test
    fun `execute with AUTO mode and enough history uses moving average`() = runTest {
        // Arrange: 3 records
        val now = System.currentTimeMillis()
        val records = listOf(
            createFastingRecordAtTime(18, 0, now - 1 * 24 * 60 * 60 * 1000L),
            createFastingRecordAtTime(18, 0, now - 2 * 24 * 60 * 60 * 1000L),
            createFastingRecordAtTime(18, 0, now - 3 * 24 * 60 * 60 * 1000L)
        )

        every { settingsRepository.smartReminderMode } returns flowOf(SmartReminderMode.AUTO)
        every { historyRepository.getAllHistory() } returns flowOf(records)

        // Act
        val result = useCase.execute()

        // Assert
        assertEquals(SuggestionSource.MOVING_AVERAGE, result.source)
        assertEquals(1080, result.suggestedTimeMinutes) // 18:00
    }

    @Test
    fun `execute with AUTO mode and not enough history uses bedtime`() = runTest {
        // Arrange: Only 1 record
        val now = System.currentTimeMillis()
        val records = listOf(
            createFastingRecordAtTime(18, 0, now - 1 * 24 * 60 * 60 * 1000L)
        )

        every { settingsRepository.smartReminderMode } returns flowOf(SmartReminderMode.AUTO)
        every { settingsRepository.bedtimeMinutes } returns flowOf(1380) // 23:00
        every { historyRepository.getAllHistory() } returns flowOf(records)

        // Act
        val result = useCase.execute()

        // Assert
        assertEquals(SuggestionSource.BEDTIME_BASED, result.source)
        assertEquals(1140, result.suggestedTimeMinutes) // 19:00 (23:00 - 4 hours)
    }

    // ============= Circular Mean Tests =============

    @Test
    fun `circular mean handles times around midnight correctly`() = runTest {
        // Arrange: Times at 23:00 and 01:00 should average to 00:00, not 12:00
        val now = System.currentTimeMillis()
        val records = listOf(
            createFastingRecordAtTime(23, 0, now - 1 * 24 * 60 * 60 * 1000L), // 23:00
            createFastingRecordAtTime(1, 0, now - 2 * 24 * 60 * 60 * 1000L),  // 01:00
            createFastingRecordAtTime(0, 0, now - 3 * 24 * 60 * 60 * 1000L)   // 00:00
        )

        every { settingsRepository.smartReminderMode } returns flowOf(SmartReminderMode.MOVING_AVERAGE_ONLY)
        every { historyRepository.getAllHistory() } returns flowOf(records)

        // Act
        val result = useCase.execute()

        // Assert
        assertEquals(SuggestionSource.MOVING_AVERAGE, result.source)
        // Should be around midnight (0 or close to 1440), not 12:00 (720)
        val avgMinutes = result.suggestedTimeMinutes
        assertTrue(
            "Expected average near midnight but got $avgMinutes",
            avgMinutes < 60 || avgMinutes > 1380
        )
    }

    @Test
    fun `filters out records older than 14 days`() = runTest {
        // Arrange: One recent record and one old record (older than 14 days)
        val now = System.currentTimeMillis()
        val records = listOf(
            createFastingRecordAtTime(18, 0, now - 1 * 24 * 60 * 60 * 1000L),   // 1 day ago (recent)
            createFastingRecordAtTime(18, 0, now - 2 * 24 * 60 * 60 * 1000L),   // 2 days ago (recent)
            createFastingRecordAtTime(12, 0, now - 20 * 24 * 60 * 60 * 1000L)   // 20 days ago (old, should be filtered)
        )

        every { settingsRepository.smartReminderMode } returns flowOf(SmartReminderMode.AUTO)
        every { settingsRepository.bedtimeMinutes } returns flowOf(1320)
        every { historyRepository.getAllHistory() } returns flowOf(records)

        // Act
        val result = useCase.execute()

        // Assert: Only 2 recent records, so should fall back to bedtime
        assertEquals(SuggestionSource.BEDTIME_BASED, result.source)
    }

    // ============= Helper Functions =============

    private fun createFastingRecordAtTime(hour: Int, minute: Int, approximateMillis: Long): FastingRecord {
        // Create a record with the specified time
        val zoneId = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(approximateMillis).atZone(zoneId).toLocalDate()
        val time = LocalTime.of(hour, minute)
        val dateTime = date.atTime(time).atZone(zoneId)
        val startTimeMillis = dateTime.toInstant().toEpochMilli()

        return FastingRecord(
            startTimeEpochMillis = startTimeMillis,
            endTimeEpochMillis = startTimeMillis + 16 * 60 * 60 * 1000L, // 16 hours later
            fastingGoalId = "16:8"
        )
    }
}
