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
    
    /**
     * Gets the start times of the most recent fasting records.
     * Used for calculating smart notification times based on user's routine.
     * @param limit Maximum number of records to retrieve (default 7)
     * @return Flow of start time timestamps in milliseconds
     */
    fun getRecentFastStartTimes(limit: Int = 7): Flow<List<Long>>
    
    suspend fun saveFastingRecord(record: FastingRecord)
}