package com.charliesbot.one.widget.common

object WidgetRefreshCalculator {
  private const val MILLIS_PER_HOUR = 60L * 60L * 1000L

  /**
   * Calculates the delay in milliseconds until the next displayed-hour boundary
   * or the exact goal boundary.
   *
   * Returns null if the fast has already reached or exceeded the goal duration.
   */
  fun calculateNextRefreshDelayMillis(
    currentTimeMillis: Long,
    startTimeMillis: Long,
    goalDurationMillis: Long,
  ): Long? {
    if (currentTimeMillis < startTimeMillis) {
      return (startTimeMillis - currentTimeMillis).coerceAtLeast(0L)
    }

    val elapsedMillis = currentTimeMillis - startTimeMillis
    if (elapsedMillis >= goalDurationMillis) {
      return null
    }

    val remainingMillis = goalDurationMillis - elapsedMillis
    val millisIntoCurrentHour = elapsedMillis % MILLIS_PER_HOUR
    val millisToNextHour =
      if (millisIntoCurrentHour == 0L) {
        MILLIS_PER_HOUR
      } else {
        MILLIS_PER_HOUR - millisIntoCurrentHour
      }

    return minOf(millisToNextHour, remainingMillis)
  }
}
