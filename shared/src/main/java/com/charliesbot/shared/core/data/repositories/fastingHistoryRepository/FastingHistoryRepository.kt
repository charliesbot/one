package com.charliesbot.shared.core.data.repositories.fastingHistoryRepository

import com.charliesbot.shared.core.data.db.FastingRecord
import com.charliesbot.shared.core.models.FastingHistoryTimePeriod
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface FastingHistoryRepository {
    fun getCurrentWeekHistory(): Flow<List<FastingRecord>>
    fun getHistoryByTimePeriod(period: FastingHistoryTimePeriod): Flow<List<FastingRecord>>
    fun getAllHistory(): Flow<List<FastingRecord>>

    /**
     * Gets the fasting history for a specific month.
     * @param yearMonth The specific month to retrieve records for.
     */
    fun getFastingsForMonth(yearMonth: YearMonth): Flow<List<FastingRecord>>
    suspend fun saveFastingRecord(record: FastingRecord)
    suspend fun deleteFastingRecord(startTimeEpochMillis: Long)
}