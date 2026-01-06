package com.charliesbot.shared.core.domain.usecase

import android.util.Log
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepository
import com.charliesbot.shared.core.data.repositories.settingsRepository.SettingsRepository
import com.charliesbot.shared.core.data.repositories.settingsRepository.SmartReminderMode
import com.charliesbot.shared.core.models.SuggestedFastingTime
import com.charliesbot.shared.core.models.SuggestionSource
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Calculates a suggested fasting start time for the user.
 *
 * Priority:
 * 1. If the user has at least [MIN_RECORDS_FOR_AVERAGE] fasting records in the last [HISTORY_DAYS] days,
 *    calculate the average start time from those records.
 * 2. Otherwise, fall back to bedtime minus [HOURS_BEFORE_BEDTIME] hours.
 */
class GetSuggestedFastingStartTimeUseCase(
    private val historyRepository: FastingHistoryRepository,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val HISTORY_DAYS = 14
        private const val MIN_RECORDS_FOR_AVERAGE = 3
        private const val HOURS_BEFORE_BEDTIME = 4
        private const val MINUTES_IN_DAY = 1440
    }

    /**
     * Execute the use case to get a suggested fasting start time for today.
     *
     * @return [SuggestedFastingTime] containing the suggested time and reasoning.
     */
    suspend fun execute(): SuggestedFastingTime {
        val mode = settingsRepository.smartReminderMode.first()
        val now = System.currentTimeMillis()
        val cutoffTime = now - (HISTORY_DAYS * 24 * 60 * 60 * 1000L)

        // Fetch all history and filter to recent records
        val allHistory = historyRepository.getAllHistory().first()
        val recentFasts = allHistory.filter { it.startTimeEpochMillis > cutoffTime }
        val hasEnoughHistory = recentFasts.size >= MIN_RECORDS_FOR_AVERAGE

        Log.d(LOG_TAG, "GetSuggestedFastingStartTimeUseCase: Mode=$mode, Found ${recentFasts.size} fasts in last $HISTORY_DAYS days")

        return when (mode) {
            SmartReminderMode.BEDTIME_ONLY -> {
                calculateFromBedtime()
            }
            SmartReminderMode.MOVING_AVERAGE_ONLY -> {
                if (hasEnoughHistory) {
                    calculateFromMovingAverage(recentFasts.map { it.startTimeEpochMillis })
                } else {
                    // Not enough data, fall back to bedtime with explanation
                    calculateFromBedtime().copy(
                        reasoning = "Not enough history (need $MIN_RECORDS_FOR_AVERAGE records). Using bedtime fallback."
                    )
                }
            }
            SmartReminderMode.FIXED_TIME -> {
                calculateFromFixedTime()
            }
            SmartReminderMode.AUTO -> {
                if (hasEnoughHistory) {
                    calculateFromMovingAverage(recentFasts.map { it.startTimeEpochMillis })
                } else {
                    calculateFromBedtime()
                }
            }
        }
    }

    /**
     * Calculate the average start time from recent fasting records.
     * Converts each start time to "minutes from midnight" and averages them.
     */
    private fun calculateFromMovingAverage(startTimes: List<Long>): SuggestedFastingTime {
        val zoneId = ZoneId.systemDefault()

        // Convert each timestamp to minutes from midnight
        val minutesFromMidnight = startTimes.map { epochMillis ->
            val localTime = Instant.ofEpochMilli(epochMillis)
                .atZone(zoneId)
                .toLocalTime()
            localTime.hour * 60 + localTime.minute
        }

        // Calculate circular mean for time-of-day (handles wrapping around midnight)
        val avgMinutes = calculateCircularMean(minutesFromMidnight)

        // Convert to today's timestamp
        val suggestedTimeMillis = minutesToTodayTimestamp(avgMinutes)

        val hours = avgMinutes / 60
        val mins = avgMinutes % 60
        val formattedTime = String.format("%02d:%02d", hours, mins)

        Log.d(LOG_TAG, "GetSuggestedFastingStartTimeUseCase: Moving average calculated as $formattedTime")

        return SuggestedFastingTime(
            suggestedTimeMillis = suggestedTimeMillis,
            suggestedTimeMinutes = avgMinutes,
            reasoning = "Based on your recent ${startTimes.size}-day average",
            source = SuggestionSource.MOVING_AVERAGE
        )
    }

    /**
     * Calculate suggested start time based on bedtime setting.
     * Suggested start = bedtime - 4 hours.
     */
    private suspend fun calculateFromBedtime(): SuggestedFastingTime {
        val bedtimeMinutes = settingsRepository.bedtimeMinutes.first()

        // Subtract 4 hours (240 minutes), handling wrap-around
        // e.g., bedtime 00:30 (30 min) -> start at 20:30 (1230 min previous day)
        var suggestedMinutes = bedtimeMinutes - (HOURS_BEFORE_BEDTIME * 60)
        if (suggestedMinutes < 0) {
            suggestedMinutes += MINUTES_IN_DAY
        }

        val suggestedTimeMillis = minutesToTodayTimestamp(suggestedMinutes)

        val bedtimeHours = bedtimeMinutes / 60
        val bedtimeMins = bedtimeMinutes % 60
        val formattedBedtime = String.format("%02d:%02d", bedtimeHours % 24, bedtimeMins)

        Log.d(LOG_TAG, "GetSuggestedFastingStartTimeUseCase: Bedtime-based calculation (bedtime: $formattedBedtime)")

        return SuggestedFastingTime(
            suggestedTimeMillis = suggestedTimeMillis,
            suggestedTimeMinutes = suggestedMinutes,
            reasoning = "Based on your $formattedBedtime bedtime (4h before)",
            source = SuggestionSource.BEDTIME_BASED
        )
    }

    /**
     * Calculate suggested start time from user's fixed time setting.
     */
    private suspend fun calculateFromFixedTime(): SuggestedFastingTime {
        val fixedMinutes = settingsRepository.fixedFastingStartMinutes.first()
        val suggestedTimeMillis = minutesToTodayTimestamp(fixedMinutes)

        val hours = fixedMinutes / 60
        val mins = fixedMinutes % 60
        val formattedTime = String.format("%02d:%02d", hours % 24, mins)

        Log.d(LOG_TAG, "GetSuggestedFastingStartTimeUseCase: Fixed time ($formattedTime)")

        return SuggestedFastingTime(
            suggestedTimeMillis = suggestedTimeMillis,
            suggestedTimeMinutes = fixedMinutes,
            reasoning = "Your scheduled fasting time",
            source = SuggestionSource.FIXED_TIME
        )
    }

    /**
     * Calculate the circular mean of time values (handles wrap-around at midnight).
     * This prevents issues like averaging 23:00 and 01:00 to get 12:00 instead of 00:00.
     */
    private fun calculateCircularMean(minutesList: List<Int>): Int {
        if (minutesList.isEmpty()) return 0

        var sinSum = 0.0
        var cosSum = 0.0

        for (minutes in minutesList) {
            val angle = (minutes.toDouble() / MINUTES_IN_DAY) * 2 * Math.PI
            sinSum += kotlin.math.sin(angle)
            cosSum += kotlin.math.cos(angle)
        }

        val avgAngle = kotlin.math.atan2(sinSum, cosSum)
        var avgMinutes = ((avgAngle / (2 * Math.PI)) * MINUTES_IN_DAY).toInt()

        // Ensure positive result
        if (avgMinutes < 0) {
            avgMinutes += MINUTES_IN_DAY
        }

        return avgMinutes
    }

    /**
     * Convert minutes from midnight to an absolute timestamp for today (or tomorrow if time has passed).
     */
    private fun minutesToTodayTimestamp(minutes: Int): Long {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val targetTime = LocalTime.of(minutes / 60, minutes % 60)
        val targetDateTime = today.atTime(targetTime).atZone(zoneId)

        val nowMillis = System.currentTimeMillis()
        val targetMillis = targetDateTime.toInstant().toEpochMilli()

        // If the time has already passed today, schedule for tomorrow
        return if (targetMillis < nowMillis) {
            today.plusDays(1).atTime(targetTime).atZone(zoneId).toInstant().toEpochMilli()
        } else {
            targetMillis
        }
    }
}

