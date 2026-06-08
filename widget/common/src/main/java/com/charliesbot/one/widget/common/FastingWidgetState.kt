package com.charliesbot.one.widget.common

import com.charliesbot.shared.core.domain.progress.calculateProgressFraction
import com.charliesbot.shared.core.models.FastingDataItem

data class FastingWidgetState(
  val isFasting: Boolean,
  val elapsedMillis: Long,
  val fastingGoalMillis: Long,
  val hoursRemaining: Long,
  val isGoalMet: Boolean,
  val progressFraction: Float,
)

fun FastingDataItem.toFastingWidgetState(
  currentTimeMillis: Long,
  fastingGoalMillis: Long,
): FastingWidgetState {
  val elapsedMillis =
    if (isFasting) {
      (currentTimeMillis - startTimeInMillis).coerceAtLeast(0)
    } else {
      0L
    }
  val hoursRemaining = millisToHours(fastingGoalMillis) - millisToHours(elapsedMillis)
  val isGoalMet = isFasting && hoursRemaining <= 0
  val progressFraction =
    if (isFasting) {
      calculateProgressFraction(elapsedMillis, fastingGoalMillis)
    } else {
      0f
    }

  return FastingWidgetState(
    isFasting = isFasting,
    elapsedMillis = elapsedMillis,
    fastingGoalMillis = fastingGoalMillis,
    hoursRemaining = hoursRemaining,
    isGoalMet = isGoalMet,
    progressFraction = progressFraction,
  )
}

private fun millisToHours(millis: Long): Long = millis / MILLIS_PER_HOUR

private const val MILLIS_PER_HOUR = 60L * 60L * 1000L
