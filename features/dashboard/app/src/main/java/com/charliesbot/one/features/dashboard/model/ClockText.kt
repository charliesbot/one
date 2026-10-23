package com.charliesbot.one.features.dashboard.model

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/** A clock time split for display: big digits, plus the AM/PM marker on 12-hour clocks. */
internal data class ClockText(val time: String, val period: String?) {
  companion object {
    fun of(time: LocalTime, is24Hour: Boolean, locale: Locale): ClockText =
      if (is24Hour) {
        ClockText(time.format(DateTimeFormatter.ofPattern("HH:mm", locale)), null)
      } else {
        ClockText(
          time = time.format(DateTimeFormatter.ofPattern("h:mm", locale)),
          period = time.format(DateTimeFormatter.ofPattern("a", locale)),
        )
      }

    /** One-line time for small labels: "6 PM", "8:30 PM", or "18:00". */
    fun compact(time: LocalTime, is24Hour: Boolean, locale: Locale): String {
      val pattern =
        when {
          is24Hour -> "HH:mm"
          time.minute == 0 -> "h a"
          else -> "h:mm a"
        }
      return time.format(DateTimeFormatter.ofPattern(pattern, locale))
    }
  }
}

/** A non-negative duration in whole hours and minutes, rounded down unless asked otherwise. */
internal data class HoursMinutes(val hours: Long, val minutes: Long) {
  companion object {
    fun of(millis: Long): HoursMinutes {
      val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis.coerceAtLeast(0L))
      return HoursMinutes(totalMinutes / 60, totalMinutes % 60)
    }

    /**
     * Like [of], but any partial minute counts as a whole one, so time left never reads zero early.
     */
    fun roundedUp(millis: Long): HoursMinutes = of(millis.coerceAtLeast(0L) + MinuteMillis - 1)
  }
}

private val MinuteMillis = TimeUnit.MINUTES.toMillis(1)
