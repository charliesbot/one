package com.charliesbot.one.widget.common

/**
 * Calculates refresh delays for widgets during an active fast.
 *
 * Computes the duration until the next whole elapsed-hour boundary or the fasting goal completion
 * timestamp, whichever occurs first.
 */
object WidgetRefreshCalculator {
  private const val MILLIS_PER_HOUR = 60L * 60L * 1000L

  /**
   * Calculates the delay in milliseconds until the next displayed-hour boundary or the exact goal
   * boundary.
   *
   * @param currentTimeMillis The current timestamp in milliseconds.
   * @param startTimeMillis The timestamp when the fast started, in milliseconds.
   * @param goalDurationMillis The total target duration of the fast in milliseconds.
   * @return The delay in milliseconds until the next scheduled refresh, or `null` if the fast has
   *   already reached or exceeded [goalDurationMillis]. If [currentTimeMillis] is earlier than
   *   [startTimeMillis], returns the duration until [startTimeMillis].
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
