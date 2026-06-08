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
  val elapsedMillis = calculateElapsedMillis(currentTimeMillis)
  val hoursRemaining = calculateHoursRemaining(fastingGoalMillis, elapsedMillis)

  return FastingWidgetState(
    isFasting = isFasting,
    elapsedMillis = elapsedMillis,
    fastingGoalMillis = fastingGoalMillis,
    hoursRemaining = hoursRemaining,
    isGoalMet = isGoalMetWith(hoursRemaining),
    progressFraction = calculateProgressFractionOrZero(elapsedMillis, fastingGoalMillis),
  )
}

private fun FastingDataItem.calculateElapsedMillis(currentTimeMillis: Long): Long =
  if (isFasting) {
    (currentTimeMillis - startTimeInMillis).coerceAtLeast(0)
  } else {
    0L
  }

private fun FastingDataItem.isGoalMetWith(hoursRemaining: Long): Boolean =
  isFasting && hoursRemaining <= 0

private fun FastingDataItem.calculateProgressFractionOrZero(
  elapsedMillis: Long,
  fastingGoalMillis: Long,
): Float =
  if (isFasting) {
    calculateProgressFraction(elapsedMillis, fastingGoalMillis)
  } else {
    0f
  }

private fun calculateHoursRemaining(fastingGoalMillis: Long, elapsedMillis: Long): Long =
  millisToHours(fastingGoalMillis) - millisToHours(elapsedMillis)

private fun millisToHours(millis: Long): Long = millis / MILLIS_PER_HOUR

private const val MILLIS_PER_HOUR = 60L * 60L * 1000L
