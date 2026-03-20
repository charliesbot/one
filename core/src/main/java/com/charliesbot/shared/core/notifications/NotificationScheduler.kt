package com.charliesbot.shared.core.notifications

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.charliesbot.shared.core.constants.AppConstants
import com.charliesbot.shared.core.constants.NotificationConstants.NOTIFICATION_FASTING_START_MILLIS_KEY
import com.charliesbot.shared.core.constants.NotificationConstants.NOTIFICATION_TYPE_KEY
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.domain.repository.SettingsRepository
import com.charliesbot.shared.core.models.NotificationType
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

class NotificationScheduler(
  private val context: Context,
  private val workerClass: Class<out ListenableWorker>,
  private val settingsRepository: SettingsRepository,
  private val workManager: WorkManager = WorkManager.getInstance(context),
) {
  companion object {
    private const val SMART_REMINDER_TAG = "smart_reminder_notification"
    private const val FASTING_NOTIFICATION_TAG = "fasting_notification"
  }

  private fun scheduleNotification(
    startMillis: Long,
    notificationDelayMillis: Long,
    type: NotificationType,
    tag: String = FASTING_NOTIFICATION_TAG,
  ) {
    val inputData =
      Data.Builder()
        .putString(NOTIFICATION_TYPE_KEY, type.name)
        .putLong(NOTIFICATION_FASTING_START_MILLIS_KEY, startMillis)
        .build()

    val notificationWork =
      OneTimeWorkRequest.Builder(workerClass)
        .setInitialDelay(notificationDelayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
        .setInputData(inputData)
        .addTag(tag)
        .addTag("notification_work_${type.name}")
        .build()

    workManager.enqueueUniqueWork(
      "notification-${type.name}",
      ExistingWorkPolicy.REPLACE,
      notificationWork,
    )
  }

  suspend fun scheduleNotifications(startMillis: Long, fastingGoalId: String) {
    cancelFastingNotifications()

    // Check if notifications are enabled
    val notificationsEnabled = settingsRepository.notificationsEnabled.first()
    if (!notificationsEnabled) {
      Log.d(
        AppConstants.LOG_TAG,
        "NotificationScheduler: Notifications disabled, skipping scheduling",
      )
      return
    }

    val endMillis = PredefinedFastingGoals.getGoalById(fastingGoalId).durationMillis
    val completeFastNotificationDelay = startMillis + endMillis - System.currentTimeMillis()
    // We show a notification 1 hour before the goal
    val almostCompleteFastNotificationDelay =
      completeFastNotificationDelay - TimeUnit.HOURS.toMillis(1)

    // Check individual notification settings
    val notifyOneHourBefore = settingsRepository.notifyOneHourBefore.first()
    val notifyOnCompletion = settingsRepository.notifyOnCompletion.first()

    if (notifyOneHourBefore) {
      scheduleNotification(
        startMillis,
        almostCompleteFastNotificationDelay.coerceAtLeast(0),
        NotificationType.ONE_HOUR_BEFORE,
      )
    }

    if (notifyOnCompletion) {
      scheduleNotification(
        startMillis,
        completeFastNotificationDelay.coerceAtLeast(0),
        NotificationType.COMPLETION,
      )
    }
  }

  /**
   * Schedule smart reminder notifications for when to start fasting. Checks DataStore for whether
   * smart reminders are enabled before scheduling.
   *
   * @param suggestedStartTimeMillis The absolute timestamp when the fast should start.
   */
  suspend fun scheduleSmartReminderNotifications(suggestedStartTimeMillis: Long) {
    cancelSmartReminderNotifications()

    // Check if smart reminders are enabled
    val smartRemindersEnabled = settingsRepository.smartRemindersEnabled.first()
    if (!smartRemindersEnabled) {
      Log.d(
        AppConstants.LOG_TAG,
        "NotificationScheduler: Smart reminders disabled, skipping scheduling",
      )
      return
    }

    scheduleSmartReminderWork(suggestedStartTimeMillis)
  }

  /**
   * Schedule smart reminder notifications without checking DataStore settings. Use this when the
   * caller has already verified that smart reminders are enabled (e.g., from a sync event that
   * includes the setting alongside the reminder data).
   *
   * @param suggestedStartTimeMillis The absolute timestamp when the fast should start.
   */
  fun scheduleSmartReminderNotificationsForced(suggestedStartTimeMillis: Long) {
    cancelSmartReminderNotifications()
    scheduleSmartReminderWork(suggestedStartTimeMillis)
  }

  private fun scheduleSmartReminderWork(suggestedStartTimeMillis: Long) {
    Log.d(
      AppConstants.LOG_TAG,
      "NotificationScheduler: Smart reminders enabled, scheduling for $suggestedStartTimeMillis",
    )

    val now = System.currentTimeMillis()
    val delayToStart = suggestedStartTimeMillis - now
    val delayTo1HourBefore = delayToStart - TimeUnit.HOURS.toMillis(1)

    Log.d(
      AppConstants.LOG_TAG,
      "NotificationScheduler: Scheduling smart reminders - Start in ${delayToStart / 60000}min, 1h before in ${delayTo1HourBefore / 60000}min",
    )

    // Schedule "1 hour left to eat" notification
    if (delayTo1HourBefore > 0) {
      scheduleNotification(
        suggestedStartTimeMillis,
        delayTo1HourBefore,
        NotificationType.SMART_REMINDER_1H_BEFORE,
        SMART_REMINDER_TAG,
      )
    }

    // Schedule "Time to start your fast" notification
    if (delayToStart > 0) {
      scheduleNotification(
        suggestedStartTimeMillis,
        delayToStart,
        NotificationType.SMART_REMINDER_START,
        SMART_REMINDER_TAG,
      )
    }
  }

  /** Cancel only fasting progress notifications (not smart reminders). */
  fun cancelFastingNotifications() {
    workManager.cancelAllWorkByTag(FASTING_NOTIFICATION_TAG)
  }

  /** Cancel only smart reminder notifications. */
  fun cancelSmartReminderNotifications() {
    workManager.cancelAllWorkByTag(SMART_REMINDER_TAG)
  }

  fun cancelAllNotifications() {
    workManager.cancelAllWork()
  }
}
