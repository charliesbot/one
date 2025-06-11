package com.charliesbot.one.data

import com.charliesbot.shared.core.data.db.FastingRecord
import com.charliesbot.shared.core.data.db.FastingRecordDao
import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepository
import com.charliesbot.shared.core.models.FastingHistoryTimePeriod
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FastingHistoryRepositoryImpl(
    private val fastingRecordDao: FastingRecordDao
) : FastingHistoryRepository {
    override suspend fun getAllHistory(): Flow<List<FastingRecord>> {
        return fastingRecordDao.getAllFastings()
    }

    override suspend fun getHistoryByTimePeriod(period: FastingHistoryTimePeriod): Flow<List<FastingRecord>> {
        val calendar = Calendar.getInstance()
        when (period) {
            FastingHistoryTimePeriod.WEEK -> calendar.add(Calendar.DAY_OF_YEAR, -7)
            FastingHistoryTimePeriod.MONTH -> calendar.add(Calendar.MONTH, -1)
            FastingHistoryTimePeriod.YEAR -> calendar.add(Calendar.YEAR, -1)
        }

        val sinceTimestamp = calendar.timeInMillis
        return fastingRecordDao.getFastingsSince(sinceTimestamp)
    }

    override suspend fun saveFastingRecord(record: FastingRecord) {
        fastingRecordDao.insert(record)
    }

}