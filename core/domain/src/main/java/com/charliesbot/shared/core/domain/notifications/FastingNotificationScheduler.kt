package com.charliesbot.shared.core.domain.notifications

interface FastingNotificationScheduler {
  suspend fun scheduleNotifications(startMillis: Long, fastingGoalId: String)

  fun cancelAllNotifications()
}
