package com.charliesbot.shared.core.utils

import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.data.db.FastingRecord
import com.charliesbot.shared.core.models.TimePeriodProgress
import com.charliesbot.shared.core.models.TimePeriodType
import java.util.Calendar

object FastingProgressCalculator {
    fun calculateWeeklyProgress(records: List<FastingRecord>): List<TimePeriodProgress> {
        val calendar = Calendar.getInstance()
        val recordsByDay = records.groupBy { record ->
            calendar.timeInMillis = record.endTimeEpochMillis
            calendar.get(Calendar.DAY_OF_WEEK)
        }

        return listOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY
        ).map { dayOfWeek ->
            val dayRecords = recordsByDay[dayOfWeek] ?: emptyList()
            // Find longest fast for this day
            val longestFastHours: Float = dayRecords.maxOfOrNull { record ->
                val durationMillis = record.endTimeEpochMillis - record.startTimeEpochMillis
                durationMillis / (1000 * 60f * 60f) // Convert to hours
            } ?: 0f

            // We consider a fasting of over 13 hours as completed
            val completedFasting = longestFastHours >= PredefinedFastingGoals.MIN_FASTING_HOURS
            val progress = if (completedFasting) 1.0f else 0.0f

            TimePeriodProgress(
                period = dayOfWeek,
                periodType = TimePeriodType.DAY_OF_WEEK,
                progress = progress,
                completedFasts = dayRecords.size,
                totalFastingHours = longestFastHours
            )
        }
    }
}