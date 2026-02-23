package com.charliesbot.shared.core.data.repositories.fastingDataRepository

import com.charliesbot.shared.core.models.FastingDataItem
import kotlinx.coroutines.flow.Flow

interface FastingDataRepository {
    val isFasting: Flow<Boolean>
    val startTimeInMillis: Flow<Long>
    val fastingGoalId: Flow<String>
    val lastUpdateTimestamp: Flow<Long>
    val fastingDataItem: Flow<FastingDataItem>

    suspend fun getCurrentFasting(): FastingDataItem?
    suspend fun startFasting(
        startTimeInMillis: Long,
        fastingGoalId: String
    ): Pair<FastingDataItem?, FastingDataItem>

    suspend fun updateFastingConfig(
        startTimeInMillis: Long? = null,
        fastingGoalId: String? = null
    ): Pair<FastingDataItem, FastingDataItem>

    suspend fun stopFasting(fastingGoalId: String): Pair<FastingDataItem, FastingDataItem>
    suspend fun updateFastingStatusFromRemote(
        startTimeInMillis: Long,
        fastingGoalId: String,
        isFasting: Boolean,
        lastUpdateTimestamp: Long
    )
}