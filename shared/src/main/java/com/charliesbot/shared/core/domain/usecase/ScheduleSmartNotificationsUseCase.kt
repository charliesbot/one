package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.notifications.NotificationScheduler
import kotlinx.coroutines.flow.first

/**
 * Schedules smart reminder notifications based on calculated optimal times.
 * 
 * Schedules two types of notifications:
 * 1. SMART_REMINDER - at the calculated optimal fasting start time
 * 2. EATING_WINDOW_CLOSING - 1 hour before the smart reminder (for daily fasts only)
 * 
 * Skips eating window closing notification for 36h fasts since they don't follow
 * a daily eating pattern.
 */
class ScheduleSmartNotificationsUseCase(
    private val calculateSmartNotificationTimeUseCase: CalculateSmartNotificationTimeUseCase,
    private val notificationScheduler: NotificationScheduler,
    private val fastingDataRepository: FastingDataRepository
) {
    companion object {
        private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L
    }

    suspend operator fun invoke() {
        // Get the user's current fasting goal
        val fastingGoalId = fastingDataRepository.fastingGoalId.first()
        
        // Calculate next notification time
        val nextNotificationTime = calculateSmartNotificationTimeUseCase()
        val triggerTimeMillis = nextNotificationTime.toInstant().toEpochMilli()
        
        // Always schedule the smart reminder
        notificationScheduler.scheduleSmartReminder(triggerTimeMillis, fastingGoalId)
        
        // Schedule eating window closing notification 1 hour before
        // Skip for 36h fasts since they don't follow a daily pattern
        if (!is36HourFast(fastingGoalId)) {
            val eatingWindowClosingTime = triggerTimeMillis - ONE_HOUR_MILLIS
            
            // Only schedule if it's in the future
            if (eatingWindowClosingTime > System.currentTimeMillis()) {
                notificationScheduler.scheduleEatingWindowClosing(eatingWindowClosingTime, fastingGoalId)
            }
        }
    }

    private fun is36HourFast(goalId: String): Boolean {
        return goalId == PredefinedFastingGoals.THIRTY_SIX_HOUR.id
    }
}

