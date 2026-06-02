package com.charliesbot.shared.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.charliesbot.shared.core.models.FastingRecord

/** Room entity for the `fasting_history` table. */
@Entity(tableName = "fasting_history")
data class FastingRecordEntity(
  @PrimaryKey val startTimeEpochMillis: Long,
  val endTimeEpochMillis: Long,
  val fastingGoalId: String,
)

fun FastingRecordEntity.toModel(): FastingRecord =
  FastingRecord(
    startTimeEpochMillis = startTimeEpochMillis,
    endTimeEpochMillis = endTimeEpochMillis,
    fastingGoalId = fastingGoalId,
  )

fun FastingRecord.toEntity(): FastingRecordEntity =
  FastingRecordEntity(
    startTimeEpochMillis = startTimeEpochMillis,
    endTimeEpochMillis = endTimeEpochMillis,
    fastingGoalId = fastingGoalId,
  )
