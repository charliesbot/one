package com.charliesbot.shared.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingRecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(fastingRecord: FastingRecord)

    @Query("SELECT * FROM fasting_history ORDER BY endTimeEpochMillis DESC")
    fun getAllFastings(): Flow<List<FastingRecord>>

    @Query("SELECT * FROM fasting_history WHERE endTimeEpochMillis >= :sinceTimestamp ORDER BY endTimeEpochMillis DESC")
    fun getFastingsSince(sinceTimestamp: Long): Flow<List<FastingRecord>>

    /**
     * Gets all fastings completed within a specific time range.
     * Note: startTimestamp is inclusive, endExclusiveTimestamp is exclusive.
     */
    @Query(
    "SELECT * FROM fasting_history " +
    "WHERE endTimeEpochMillis >= :startTimestamp AND endTimeEpochMillis < :endExclusiveTimestamp " +
    "ORDER BY endTimeEpochMillis DESC"
    )
    fun getFastingsForPeriod(
        startTimestamp: Long,
        endExclusiveTimestamp: Long
    ): Flow<List<FastingRecord>>

}