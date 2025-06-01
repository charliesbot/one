package com.charliesbot.shared.core.data.repositories.fastingDataRepository

import com.charliesbot.shared.core.models.FastingDataItem
import kotlinx.coroutines.flow.Flow

interface FastingDataRepository {
    val isFasting: Flow<Boolean>
    val startTimeInMillis: Flow<Long>
    val fastingGoalId: Flow<String>
    val lastUpdateTimestamp: Flow<Long>

    suspend fun getCurrentFasting(): FastingDataItem?
    suspend fun startFasting(startTimeInMillis: Long)
    suspend fun updateFastingSchedule(startTimeInMillis: Long)
    suspend fun updateFastingGoalId(fastingGoalId: String)
    suspend fun stopFasting()
    suspend fun updateFastingStatusFromRemote(
        startTimeInMillis: Long,
        fastingGoalId: String,
        isFasting: Boolean,
        lastUpdateTimestamp: Long
    )
}