package com.charliesbot.shared.core.data.repositories.fastingDataRepository

import com.charliesbot.shared.core.models.FastingDataItem
import kotlinx.coroutines.flow.Flow

interface FastingDataRepository {
    val isFasting: Flow<Boolean>
    val startTimeInMillis: Flow<Long>
    val lastUpdateTimestamp: Flow<Long>

    suspend fun getCurrentFasting(): FastingDataItem?
    suspend fun startFasting(startTimeInMillis: Long)
    suspend fun updateStartTime(startTimeInMillis: Long)
    suspend fun stopFasting()
    suspend fun updateFastingStatusFromRemote(
        startTimeInMillis: Long,
        isFasting: Boolean,
        lastUpdateTimestamp: Long
    )
}