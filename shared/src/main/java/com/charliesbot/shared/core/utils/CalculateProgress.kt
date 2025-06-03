package com.charliesbot.shared.core.utils

import com.charliesbot.shared.core.constants.AppConstants

fun calculateProgressFraction(progressMillis: Long, totalDurationGoalMillis: Long?): Float {
    if (totalDurationGoalMillis == null || totalDurationGoalMillis <= 0) {
        throw Error(AppConstants.LOG_TAG + "totalDurationGoalMillis cannot be null or less than or equal to 0")
    }
    return (progressMillis.toFloat() / totalDurationGoalMillis).coerceIn(0f, 1f)
}

fun calculateProgressPercentage(progressMillis: Long, totalDurationGoalMillis: Long?): Int {
    if (totalDurationGoalMillis == null || totalDurationGoalMillis <= 0) {
        throw Error(AppConstants.LOG_TAG + "totalDurationGoalMillis cannot be null or less than or equal to 0")
    }
    val progressFraction = calculateProgressFraction(progressMillis, totalDurationGoalMillis)
    return progressFraction.times(100).toInt()
}