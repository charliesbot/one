package com.charliesbot.shared.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fasting_history")
data class FastingRecord(
    @PrimaryKey
    val startTimeEpochMillis: Long,

    val endTimeEpochMillis: Long,
    val fastingGoalId: String,
)