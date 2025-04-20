package com.charliesbot.shared.core.utils

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

fun getHours(millis: Long): Long {
    return millis / (1000 * 60 * 60)
}

fun formatTimestamp(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = getHours(millis)
    return String.format(
        Locale.US,
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