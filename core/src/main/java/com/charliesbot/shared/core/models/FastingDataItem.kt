package com.charliesbot.shared.core.models

/**
 * Represents the *current* fasting state of the user.
 *
 * This data class is the primary model for the user's real-time status. It is
 * persisted in Jetpack DataStore and serves as the single source of truth
 * for the application's UI.
 *
 * Its properties are also the data that is synced between devices (e.g., phone and watch)
 * via the Wearable Data Layer to ensure state consistency.
 *
 * This is distinct from [com.charliesbot.shared.core.data.db.FastingRecord], which represents a *completed, historical* fast.
 *
 * @property isFasting True if the user is currently fasting, false otherwise.
 * @property startTimeInMillis The timestamp (in UTC milliseconds) when the *current* fast began.
 * This is 0 or irrelevant if [isFasting] is false.
 * @property updateTimestamp The timestamp (in UTC milliseconds) when this state was last updated,
 * used for data freshness checks and synchronization.
 * @property fastingGoalId An identifier string for the *current* fasting goal (e.g., "16:8").
 */
data class FastingDataItem(
    val isFasting: Boolean = false,
    val startTimeInMillis: Long = 0L,
    val updateTimestamp: Long = 0L,
    val fastingGoalId: String = "",
)