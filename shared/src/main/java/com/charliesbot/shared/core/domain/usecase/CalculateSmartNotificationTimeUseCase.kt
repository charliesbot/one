package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepository
import com.charliesbot.shared.core.data.repositories.preferencesRepository.PreferencesRepository
import com.charliesbot.shared.core.utils.calculateCircularMean
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Calculate the optimal time for smart fasting reminder notifications.
 * 
 * Priority fallback strategy:
 * 1. Moving average of past 7 daily fasts (if 7+ records exist, excluding 36h fasts)
 * 2. Bedtime minus 6 hours (if bedtime is set in preferences)
 * 3. Default fallback: 8 PM (20:00)
 * 
 * @return ZonedDateTime for the next notification trigger (today or tomorrow)
 */
class CalculateSmartNotificationTimeUseCase(
    private val fastingHistoryRepository: FastingHistoryRepository,
    private val preferencesRepository: PreferencesRepository
) {
    companion object {
        private const val REQUIRED_HISTORY_COUNT = 7
        private const val MAX_DAILY_FAST_HOURS = 30 // Exclude 36h fasts from average
        private const val DEFAULT_HOUR = 20 // 8 PM fallback
        private const val BEDTIME_OFFSET_HOURS = 6 // Notify 6 hours before bedtime
    }

    suspend operator fun invoke(): ZonedDateTime {
        val now = ZonedDateTime.now()
        val notificationTime = calculateNotificationTime()
        
        // Calculate next occurrence (today or tomorrow)
        var nextTrigger = now
            .toLocalDate()
            .atTime(notificationTime)
            .atZone(now.zone)
        
        // If the time has already passed today, schedule for tomorrow
        if (nextTrigger.isBefore(now) || nextTrigger.isEqual(now)) {
            nextTrigger = nextTrigger.plusDays(1)
        }
        
        return nextTrigger
    }

    private suspend fun calculateNotificationTime(): LocalTime {
        // Priority 1: Moving average of recent fasts
        val recentStartTimes = fastingHistoryRepository.getRecentFastStartTimes(REQUIRED_HISTORY_COUNT).first()
        
        if (recentStartTimes.size >= REQUIRED_HISTORY_COUNT) {
            val dailyFastTimes = filterDailyFasts(recentStartTimes)
            
            if (dailyFastTimes.size >= REQUIRED_HISTORY_COUNT) {
                val localTimes = dailyFastTimes.map { millis ->
                    Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime()
                }
                return calculateCircularMean(localTimes)
            }
        }
        
        // Priority 2: Bedtime-based calculation
        val bedtime = preferencesRepository.getBedtime().first()
        if (bedtime != null) {
            return bedtime.minusHours(BEDTIME_OFFSET_HOURS.toLong())
        }
        
        // Priority 3: Default fallback
        return LocalTime.of(DEFAULT_HOUR, 0)
    }

    /**
     * Filter out 36h fasts to avoid skewing daily fast averages.
     * Keeps only fasts shorter than MAX_DAILY_FAST_HOURS.
     */
    private fun filterDailyFasts(startTimes: List<Long>): List<Long> {
        if (startTimes.size < 2) return startTimes
        
        return startTimes.zipWithNext().mapNotNull { (current, previous) ->
            val durationHours = (current - previous) / (1000 * 60 * 60)
            if (durationHours < MAX_DAILY_FAST_HOURS) {
                current
            } else {
                null
            }
        }
    }
}

