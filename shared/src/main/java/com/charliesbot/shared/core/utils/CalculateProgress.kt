package com.charliesbot.shared.core.utils

const val TOTAL_FASTING_TIME_MILLIS = 16 * 60 * 60 * 1000L

fun calculateProgressFraction(progressMillis: Long): Float {
    return (progressMillis.toFloat() / TOTAL_FASTING_TIME_MILLIS).coerceIn(0f, 1f)
}

fun calculateProgressPercentage(progressMillis: Long): Int {
    val progressFraction = calculateProgressFraction(progressMillis)
    return progressFraction.times(100).toInt()
}