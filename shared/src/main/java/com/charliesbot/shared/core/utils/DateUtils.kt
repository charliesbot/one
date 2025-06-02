package com.charliesbot.shared.core.utils

import android.text.format.DateUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimeFormat(val pattern: String) {
    DATE_TIME("EEE, h:mm a"),
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