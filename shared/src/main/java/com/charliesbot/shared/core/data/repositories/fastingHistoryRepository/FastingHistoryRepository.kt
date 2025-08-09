package com.charliesbot.shared.core.data.repositories.fastingHistoryRepository

import com.charliesbot.shared.core.data.db.FastingRecord
import com.charliesbot.shared.core.models.FastingHistoryTimePeriod
import kotlinx.coroutines.flow.Flow

interface FastingHistoryRepository {
    fun getCurrentWeekHistory(): Flow<List<FastingRecord>>
    fun getHistoryByTimePeriod(period: FastingHistoryTimePeriod): Flow<List<FastingRecord>>
    fun getAllHistory(): Flow<List<FastingRecord>>
    suspend fun saveFastingRecord(record: FastingRecord)
}