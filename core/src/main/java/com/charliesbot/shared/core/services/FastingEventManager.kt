package com.charliesbot.shared.core.services

import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.notifications.NotificationScheduler

class FastingEventManager(private val notificationScheduler: NotificationScheduler) {
  /**
   * Intelligently processes any fasting state change by comparing the state before and after the
   * event. This centralizes all business rule decisions.
   *
   * @param previousItem The state of the fast BEFORE the change. Can be null if no fast existed.
   * @param currentItem The state of the fast AFTER the change.
   * @param callbacks The platform-specific implementation that will handle the callbacks.
   */
  suspend fun processStateChange(
    previousItem: FastingDataItem?,
    currentItem: FastingDataItem,
    callbacks: FastingEventCallbacks,
  ) {
    val wasFasting = previousItem?.isFasting == true
    val isNowFasting = currentItem.isFasting

    when {
      // Case 1: A new fast is starting (false -> true)
      !wasFasting && isNowFasting -> {
        notificationScheduler.scheduleNotifications(
          currentItem.startTimeInMillis,
          currentItem.fastingGoalId,
        )
        callbacks.onFastingStarted(currentItem)
      }

      // Case 2: An existing fast has stopped (true -> false)
      wasFasting && !isNowFasting -> {
        notificationScheduler.cancelAllNotifications()
        callbacks.onFastingCompleted(currentItem)
      }

      // Case 3: An *active* fast was updated (true -> true)
      wasFasting && isNowFasting -> {
        // Reschedule notifications with the new config
        notificationScheduler.cancelAllNotifications()
        notificationScheduler.scheduleNotifications(
          currentItem.startTimeInMillis,
          currentItem.fastingGoalId,
        )
        callbacks.onFastingUpdated(currentItem)
      }

      // Case 4: An *inactive* fast's config was updated (false -> false)
      else -> {
        // No notifications needed, but we still trigger the UI update callback.
        callbacks.onFastingUpdated(currentItem)
      }
    }
  }
}
