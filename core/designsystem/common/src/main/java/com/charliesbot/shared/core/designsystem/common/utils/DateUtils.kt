package com.charliesbot.shared.core.designsystem.common.utils

import android.text.format.DateUtils
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

enum class TimeFormat(val pattern: String) {
  DATE_TIME("EEE, h:mm a"),
  DATE("MMMM d yyyy"),
  MONTH_DAY("MMMM d"),
  TIME("h:mm a"),
  DURATION("HH:mm:ss"),
}

fun formatDate(date: LocalDateTime, format: TimeFormat = TimeFormat.DATE_TIME): String {
  val formatter = DateTimeFormatter.ofPattern(format.pattern, Locale.ENGLISH)
  return date.format(formatter)
}

fun getHours(millis: Long?): Long {
  if (millis == null) return 0
  return millis / (1000 * 60 * 60)
}

fun formatTimestamp(millis: Long): String {
  val seconds = (millis / 1000) % 60
  val minutes = (millis / (1000 * 60)) % 60
  val hours = getHours(millis)
  return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

fun convertLocalDateTimeToMillis(dateTime: LocalDateTime): Long =
  dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun formatTimestamp(millis: Long, format: TimeFormat = TimeFormat.DURATION): String {
  val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
  return formatDate(dateTime, format)
}

fun convertMillisToLocalDateTime(millis: Long): LocalDateTime =
  LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())

fun getFormattedRelativeTime(startTimeMillis: Long): String {
  val nowMillis = System.currentTimeMillis()
  val minResolution = DateUtils.MINUTE_IN_MILLIS
  val flags = DateUtils.FORMAT_ABBREV_RELATIVE

  return DateUtils.getRelativeTimeSpanString(startTimeMillis, nowMillis, minResolution, flags)
    .toString()
}

fun formatMinutesAsTime(minutes: Int): String {
  val calendar =
    Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, minutes / 60)
      set(Calendar.MINUTE, minutes % 60)
    }
  val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
  return timeFormat.format(calendar.time)
}
