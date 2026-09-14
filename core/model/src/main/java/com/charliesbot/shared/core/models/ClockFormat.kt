package com.charliesbot.shared.core.models

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimeFormatMode {
  SYSTEM,
  TWELVE_HOUR,
  TWENTY_FOUR_HOUR;

  fun is24Hour(system24Hour: Boolean): Boolean =
    when (this) {
      SYSTEM -> system24Hour
      TWELVE_HOUR -> false
      TWENTY_FOUR_HOUR -> true
    }

  companion object {
    fun fromStoredValue(value: String?): TimeFormatMode =
      entries.find { it.name == value } ?: SYSTEM
  }
}

/** Clock presentation only. Never use this for durations or serialized timestamps. */
data class ClockFormat(val is24Hour: Boolean, val locale: Locale) {
  private val timePattern: String
    get() = if (is24Hour) "HH:mm" else "h:mm a"

  fun time(value: LocalTime): String =
    value.format(DateTimeFormatter.ofPattern(timePattern, locale))

  fun minutes(value: Int): String = time(LocalTime.of(value / 60, value % 60))

  fun dateTime(value: LocalDateTime, datePattern: String = "EEE"): String =
    value.format(DateTimeFormatter.ofPattern("$datePattern, $timePattern", locale))
}
