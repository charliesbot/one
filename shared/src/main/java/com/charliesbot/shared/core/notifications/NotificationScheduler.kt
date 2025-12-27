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
import com.charliesbot.shared.core.data.repositories.settingsRepository.SettingsRepository
import com.charliesbot.shared.core.models.NotificationType
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class NotificationScheduler(
    private val context: Context,
    private val workerClass: Class<out ListenableWorker>,
    private val settingsRepository: SettingsRepository,
) {
    companion object {
        private const val SMART_REMINDER_TAG = "smart_reminder_notification"
        private const val FASTING_NOTIFICATION_TAG = "fasting_notification"
    }

    private fun scheduleNotification(
        startMillis: Long,
        notificationDelayMillis: Long,
        type: NotificationType,
        tag: String = FASTING_NOTIFICATION_TAG
    ) {
        val inputData =
            Data.Builder()
                .putString(NOTIFICATION_TYPE_KEY, type.name)
                .putLong(NOTIFICATION_FASTING_START_MILLIS_KEY, startMillis)
                .build()

        val notificationWork = OneTimeWorkRequest.Builder(workerClass)
            .setInitialDelay(notificationDelayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(tag)
            .addTag("notification_work_${type.name}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "notification-${type.name}",
            ExistingWorkPolicy.REPLACE,
            notificationWork
        )
    }

    suspend fun scheduleNotifications(startMillis: Long, fastingGoalId: String) {
        cancelFastingNotifications()
        
        // Check if notifications are enabled
        val notificationsEnabled = settingsRepository.notificationsEnabled.first()
        if (!notificationsEnabled) {
            Log.d(AppConstants.LOG_TAG, "NotificationScheduler: Notifications disabled, skipping scheduling")
            return
        }
        
        val endMillis = PredefinedFastingGoals.getGoalById(fastingGoalId).durationMillis
        val completeFastNotificationDelay =
            startMillis + endMillis - System.currentTimeMillis()
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
                NotificationType.ONE_HOUR_BEFORE
            )
        }
        
        if (notifyOnCompletion) {
            scheduleNotification(
                startMillis,
                completeFastNotificationDelay.coerceAtLeast(0),
                NotificationType.COMPLETION
            )
        }
    }

    /**
     * Schedule smart reminder notifications for when to start fasting.
     *
     * @param suggestedStartTimeMillis The absolute timestamp when the fast should start.
     */
    suspend fun scheduleSmartReminderNotifications(suggestedStartTimeMillis: Long) {
        cancelSmartReminderNotifications()

        // Check if smart reminders are enabled
        val smartRemindersEnabled = settingsRepository.smartRemindersEnabled.first()
        if (!smartRemindersEnabled) {
            Log.d(AppConstants.LOG_TAG, "NotificationScheduler: Smart reminders disabled, skipping scheduling")
            return
        }
        Log.d(AppConstants.LOG_TAG, "NotificationScheduler: Smart reminders enabled, scheduling for $suggestedStartTimeMillis")

        val now = System.currentTimeMillis()
        val delayToStart = suggestedStartTimeMillis - now
        val delayTo1HourBefore = delayToStart - TimeUnit.HOURS.toMillis(1)

        Log.d(AppConstants.LOG_TAG, "NotificationScheduler: Scheduling smart reminders - Start in ${delayToStart / 60000}min, 1h before in ${delayTo1HourBefore / 60000}min")

        // Schedule "1 hour left to eat" notification
        if (delayTo1HourBefore > 0) {
            scheduleNotification(
                suggestedStartTimeMillis,
                delayTo1HourBefore,
                NotificationType.SMART_REMINDER_1H_BEFORE,
                SMART_REMINDER_TAG
            )
        }

        // Schedule "Time to start your fast" notification
        if (delayToStart > 0) {
            scheduleNotification(
                suggestedStartTimeMillis,
                delayToStart,
                NotificationType.SMART_REMINDER_START,
                SMART_REMINDER_TAG
            )
        }
    }

    /**
     * Cancel only fasting progress notifications (not smart reminders).
     */
    fun cancelFastingNotifications() {
        WorkManager.getInstance(context).cancelAllWorkByTag(FASTING_NOTIFICATION_TAG)
    }

    /**
     * Cancel only smart reminder notifications.
     */
    fun cancelSmartReminderNotifications() {
        WorkManager.getInstance(context).cancelAllWorkByTag(SMART_REMINDER_TAG)
    }

    fun cancelAllNotifications() {
        WorkManager.getInstance(context).cancelAllWork()
    }
}