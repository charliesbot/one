package com.charliesbot.shared.core.utils

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
    DURATION("HH:mm:ss")
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
    return String.format(
        Locale.getDefault(),
        "%02d:%02d:%02d", hours, minutes, seconds
    )
}

fun convertLocalDateTimeToMillis(dateTime: LocalDateTime): Long {
    return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

fun formatTimestamp(millis: Long, format: TimeFormat = TimeFormat.DURATION): String {
    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
    return formatDate(dateTime, format)
}

fun convertMillisToLocalDateTime(millis: Long): LocalDateTime {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
}


fun getFormattedRelativeTime(
    startTimeMillis: Long
): String {
    val nowMillis = System.currentTimeMillis()
    val minResolution = DateUtils.MINUTE_IN_MILLIS

    // Use DateUtils.FORMAT_ABBREV_RELATIVE for shorter text like "5 min. ago" or "Yesterday".
    val flags = DateUtils.FORMAT_ABBREV_RELATIVE // Or pass 0 for non-abbreviated

    return DateUtils.getRelativeTimeSpanString(
        startTimeMillis,
        nowMillis,
        minResolution,
        flags
    ).toString()
}

/**
 * Formats minutes from midnight as a locale-aware time string.
 * Uses 12-hour or 24-hour format based on user's system settings.
 *
 * @param minutes Minutes from midnight (0-1439)
 * @return Formatted time string (e.g., "8:00 PM" or "20:00" depending on locale)
 */
fun formatMinutesAsTime(minutes: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minutes / 60)
        set(Calendar.MINUTE, minutes % 60)
    }
    val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT)
    return timeFormat.format(calendar.time)
}