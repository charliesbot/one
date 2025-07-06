package com.charliesbot.shared.core.services

import android.util.Log
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.notifications.NotificationScheduler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FastingEventManager : KoinComponent {
    private val notificationScheduler: NotificationScheduler by inject()

    /**
     * Intelligently processes any fasting state change by comparing the state
     * before and after the event. This centralizes all business rule decisions.
     *
     * @param previousItem The state of the fast BEFORE the change. Can be null if no fast existed.
     * @param currentItem The state of the fast AFTER the change.
     * @param callbacks The platform-specific implementation that will handle the callbacks.
     */
    suspend fun processStateChange(
        previousItem: FastingDataItem?,
        currentItem: FastingDataItem,
        callbacks: FastingEventCallbacks
    ) {
        val wasFasting = previousItem?.isFasting == true
        val isNowFasting = currentItem.isFasting

        Log.d(
            LOG_TAG,
            "EventManager: Processing state change. Was Fasting: $wasFasting, Is Now Fasting: $isNowFasting"
        )

        when {
            // Case 1: A new fast is starting (false -> true)
            !wasFasting && isNowFasting -> {
                Log.d(LOG_TAG, "EventManager: Firing onFastingStarted.")
                notificationScheduler.scheduleNotifications(
                    currentItem.startTimeInMillis,
                    currentItem.fastingGoalId
                )
                callbacks.onFastingStarted(currentItem)
            }
            // Case 2: An existing fast has stopped (true -> false)
            wasFasting && !isNowFasting -> {
                Log.d(LOG_TAG, "EventManager: Firing onFastingCompleted.")
                notificationScheduler.cancelAllNotifications()
                callbacks.onFastingCompleted(currentItem)
            }
            // Case 3: An *active* fast was updated (true -> true)
            wasFasting && isNowFasting -> {
                Log.d(LOG_TAG, "EventManager: Firing onFastingUpdated for an active fast.")
                // Reschedule notifications with the new config
                notificationScheduler.cancelAllNotifications()
                notificationScheduler.scheduleNotifications(
                    currentItem.startTimeInMillis,
                    currentItem.fastingGoalId
                )
                callbacks.onFastingUpdated(currentItem)
            }
            // Case 4: An *inactive* fast's config was updated (false -> false)
            else -> {
                Log.d(LOG_TAG, "EventManager: Firing onFastingUpdated for an inactive fast.")
                // No notifications needed, but we still trigger the UI update callback.
                callbacks.onFastingUpdated(currentItem)
            }
        }
    }

}