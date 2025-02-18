package com.charliesbot.one.core.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimeFormat(val pattern: String) {
    DATE_TIME("EEE, h:mm a"),
    DURATION("HH:mm:ss")
}

fun formatDate(date: LocalDateTime, format: TimeFormat = TimeFormat.DATE_TIME): String {
    val formatter = DateTimeFormatter.ofPattern(format.pattern, Locale.ENGLISH)
    return date.format(formatter)
}

fun formatTimestamp(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)
    return String.format(
        Locale.US,
        "%02d:%02d:%02d", hours, minutes, seconds
    )
}
