package com.charliesbot.shared.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingRecordDao {
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(fastingRecord: FastingRecordEntity)

  @Query("SELECT * FROM fasting_history ORDER BY endTimeEpochMillis DESC")
  fun getAllFastings(): Flow<List<FastingRecordEntity>>

  @Query(
    "SELECT * FROM fasting_history WHERE endTimeEpochMillis >= :sinceTimestamp ORDER BY endTimeEpochMillis DESC"
  )
  fun getFastingsSince(sinceTimestamp: Long): Flow<List<FastingRecordEntity>>

  /**
   * Gets all fastings completed within a specific time range. Note: startTimestamp is inclusive,
   * endExclusiveTimestamp is exclusive.
   */
  @Query(
    "SELECT * FROM fasting_history " +
      "WHERE endTimeEpochMillis >= :startTimestamp AND endTimeEpochMillis < :endExclusiveTimestamp " +
      "ORDER BY endTimeEpochMillis DESC"
  )
  fun getFastingsForPeriod(
    startTimestamp: Long,
    endExclusiveTimestamp: Long,
  ): Flow<List<FastingRecordEntity>>

  @Query("DELETE FROM fasting_history WHERE startTimeEpochMillis = :startTimeEpochMillis")
  suspend fun deleteByStartTime(startTimeEpochMillis: Long)
}
