package com.charliesbot.shared.core.testing

import com.charliesbot.shared.core.models.TimePeriodProgress
import com.charliesbot.shared.core.models.TimePeriodType
import java.util.Calendar

object MockDataUtils {
    
    fun createMockWeeklyProgress(): List<TimePeriodProgress> {
        return listOf(
            TimePeriodProgress(
                period = Calendar.MONDAY,
                periodType = TimePeriodType.DAY_OF_WEEK,
                progress = 1.0f,
                completedFasts = 1
            ),
            TimePeriodProgress(
                period = Calendar.TUESDAY,
                periodType = TimePeriodType.DAY_OF_WEEK,
                progress = 0.7f,
                completedFasts = 1
            ),
            TimePeriodProgress(
                period = Calendar.WEDNESDAY,
                periodType = TimePeriodType.DAY_OF_WEEK,
                progress = 0.1f,
                completedFasts = 0
            ),
            TimePeriodProgress(
                period = Calendar.THURSDAY,
                periodType = TimePeriodType.DAY_OF_WEEK,
                progress = 1.0f,
                completedFasts = 2
            ),
            TimePeriodProgress(
                period = Calendar.FRIDAY,
                periodType = TimePeriodType.DAY_OF_WEEK,
                progress = 0.5f,
                completedFasts = 1
            ),
            TimePeriodProgress(
                period = Calendar.SATURDAY,
                periodType = TimePeriodType.DAY_OF_WEEK,
                progress = 0.1f,
                completedFasts = 0
            ),
            TimePeriodProgress(
                period = Calendar.SUNDAY,
                periodType = TimePeriodType.DAY_OF_WEEK,
                progress = 0.9f,
                completedFasts = 1
            )
        )
    }
}