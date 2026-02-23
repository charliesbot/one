package com.charliesbot.shared.core.models

data class TimePeriodProgress(
    val period: Int, // Calendar.MONDAY, day of month, month of year, etc.
    val periodType: TimePeriodType, // DAY_OF_WEEK, DAY_OF_MONTH, MONTH_OF_YEAR
    val progress: Float,
    val completedFasts: Int = 0,
    val totalFastingHours: Float = 0f
)

enum class TimePeriodType {
    DAY_OF_WEEK,    // For weekly view
    DAY_OF_MONTH,   // For monthly view
    MONTH_OF_YEAR   // For yearly view
}

