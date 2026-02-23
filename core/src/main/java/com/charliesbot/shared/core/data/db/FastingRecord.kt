package com.charliesbot.shared.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single, completed fasting session.
 *
 * This data class is a Room [Entity] that defines the schema for the `fasting_history` table.
 * It serves as an immutable record of a past fast, characterized by a definitive
 * [startTimeEpochMillis] and [endTimeEpochMillis].
 *
 * This is distinct from [com.charliesbot.shared.core.models.FastingDataItem], which represents the *current, ongoing* fasting state.
 *
 * @property startTimeEpochMillis The timestamp (in UTC milliseconds) when the fast began. Also the Primary Key.
 * @property endTimeEpochMillis The timestamp (in UTC milliseconds) when the fast ended.
 * @property fastingGoalId An identifier string for the goal (e.g., "16:8") associated with this fast.
 */
@Entity(tableName = "fasting_history")
data class FastingRecord(
    @PrimaryKey
    val startTimeEpochMillis: Long,
    val endTimeEpochMillis: Long,
    val fastingGoalId: String,
)