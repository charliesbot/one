package com.charliesbot.shared.core.models

/**
 * Represents a single, completed fasting session.
 *
 * This pure model is used by domain and UI layers. Persistence-specific annotations belong on the
 * Room entity in the data layer.
 */
data class FastingRecord(
  val startTimeEpochMillis: Long,
  val endTimeEpochMillis: Long,
  val fastingGoalId: String,
)
