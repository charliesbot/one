package com.charliesbot.shared.core.notifications

interface FastingNotificationScheduler {
  suspend fun scheduleNotifications(startMillis: Long, fastingGoalId: String)

  fun cancelAllNotifications()
}
