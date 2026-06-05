package com.charliesbot.shared.core.domain.progress

private const val INVALID_GOAL_DURATION_MESSAGE =
  "totalDurationGoalMillis cannot be null or less than or equal to 0"

fun calculateProgressFraction(progressMillis: Long, totalDurationGoalMillis: Long?): Float {
  if (totalDurationGoalMillis == null || totalDurationGoalMillis <= 0) {
    throw Error(INVALID_GOAL_DURATION_MESSAGE)
  }
  return (progressMillis.toFloat() / totalDurationGoalMillis).coerceIn(0f, 1f)
}

fun calculateProgressPercentage(progressMillis: Long, totalDurationGoalMillis: Long?): Int {
  if (totalDurationGoalMillis == null || totalDurationGoalMillis <= 0) {
    throw Error(INVALID_GOAL_DURATION_MESSAGE)
  }
  val progressFraction = calculateProgressFraction(progressMillis, totalDurationGoalMillis)
  return progressFraction.times(100).toInt()
}
