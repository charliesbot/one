package com.charliesbot.one.data

import com.charliesbot.shared.core.data.db.FastingRecord
import com.charliesbot.shared.core.data.db.FastingRecordDao
import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepository
import com.charliesbot.shared.core.models.FastingHistoryTimePeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar

class FastingHistoryRepositoryImpl(
    private val fastingRecordDao: FastingRecordDao
) : FastingHistoryRepository {

    private fun getWeekStartDaySetting(): Int {
        // TODO: Replace with actual settings when settings view is implemented
        return Calendar.MONDAY
    }

    override fun getCurrentWeekHistory(): Flow<List<FastingRecord>> {
        // Calculate days to go back from today to reach the start of current week.
        // Sunday requires special handling because in Java Calendar
        // it's both day 1 and the last day of week.
        val calendar = Calendar.getInstance()
        val weekStartDay = getWeekStartDaySetting()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val daysFromWeekStart = when (weekStartDay) {
            Calendar.SUNDAY -> {
                if (currentDayOfWeek == Calendar.SUNDAY) 0 else currentDayOfWeek - 1
            }

            Calendar.MONDAY -> {
                if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
            }

            else -> {
                throw IllegalStateException("Unsupported week start day setting: $weekStartDay")
            }
        }

        calendar.add(Calendar.DAY_OF_YEAR, -daysFromWeekStart)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis
        return fastingRecordDao.getFastingsSince(startOfWeek)
    }

    override fun getAllHistory(): Flow<List<FastingRecord>> {
        return fastingRecordDao.getAllFastings()
    }

    override fun getHistoryByTimePeriod(period: FastingHistoryTimePeriod): Flow<List<FastingRecord>> {
        val calendar = Calendar.getInstance()
        when (period) {
            FastingHistoryTimePeriod.WEEK -> calendar.add(Calendar.DAY_OF_YEAR, -7)
            FastingHistoryTimePeriod.MONTH -> calendar.add(Calendar.MONTH, -1)
            FastingHistoryTimePeriod.YEAR -> calendar.add(Calendar.YEAR, -1)
        }

        val sinceTimestamp = calendar.timeInMillis
        return fastingRecordDao.getFastingsSince(sinceTimestamp)
    }

    override fun getFastingsForMonth(yearMonth: YearMonth): Flow<List<FastingRecord>> {
        // This is now even cleaner!

        // 1. Get the start of the given month (e.g., 2025-09-01T00:00:00)
        val startTimestamp = yearMonth.atDay(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // 2. Get the start of the NEXT month (e.g., 2025-10-01T00:00:00)
        val endExclusiveTimestamp = yearMonth.plusMonths(1)
            .atDay(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return fastingRecordDao.getFastingsForPeriod(
            startTimestamp = startTimestamp,
            endExclusiveTimestamp = endExclusiveTimestamp
        )
    }

    override fun getRecentFastStartTimes(limit: Int): Flow<List<Long>> {
        return fastingRecordDao.getRecentRecords(limit).map { records ->
            records.map { it.startTimeEpochMillis }
        }
    }

    override suspend fun saveFastingRecord(record: FastingRecord) {
        fastingRecordDao.insert(record)
    }

}