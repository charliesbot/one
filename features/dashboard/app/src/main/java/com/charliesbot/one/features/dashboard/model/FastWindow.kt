package com.charliesbot.one.features.dashboard.model

import java.time.ZoneId

/**
 * The span of one fast, from its start to the moment its goal is reached, plus where the user's
 * bedtime falls inside it. Drives the fast-window bar and the active fast dial.
 */
internal data class FastWindow(
  val startMillis: Long,
  val endMillis: Long,
  val bedtimeMillis: Long?,
) {

  private val durationMillis: Long
    get() = endMillis - startMillis

  /** Bedtime position from 0 (start) to 1 (goal), or null when no bedtime falls inside. */
  val bedtimeFraction: Float?
    get() = bedtimeMillis?.let { (it - startMillis).toFloat() / durationMillis }

  fun progressAt(nowMillis: Long): Float =
    ((nowMillis - startMillis).toFloat() / durationMillis).coerceIn(0f, 1f)

  fun remainingAt(nowMillis: Long): Long = (endMillis - nowMillis).coerceAtLeast(0L)

  /** How far past the goal the fast has run, or 0 before the goal is reached. */
  fun overtimeAt(nowMillis: Long): Long = (nowMillis - endMillis).coerceAtLeast(0L)

  companion object {
    /**
     * @param bedtimeMinutes bedtime as minutes from midnight; the first bedtime at or after the
     *   start is used, so a 1 AM bedtime lands on the next day.
     */
    fun create(
      startMillis: Long,
      durationMillis: Long,
      bedtimeMinutes: Int?,
      zone: ZoneId,
    ): FastWindow {
      val endMillis = startMillis + durationMillis
      val bedtimeMillis =
        bedtimeMinutes?.let { nextTimeOfDay(it, startMillis, zone) }?.takeIf { it <= endMillis }
      return FastWindow(startMillis, endMillis, bedtimeMillis)
    }
  }
}
