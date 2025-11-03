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
import com.charliesbot.shared.core.models.NotificationType
import java.util.concurrent.TimeUnit

class NotificationScheduler(
    private val context: Context,
    private val workerClass: Class<out ListenableWorker>,
) {
    private fun scheduleNotification(
        startMillis: Long,
        notificationDelayMillis: Long,
        type: NotificationType
    ) {
        val inputData =
            Data.Builder()
                .putString(NOTIFICATION_TYPE_KEY, type.name)
                .putLong(NOTIFICATION_FASTING_START_MILLIS_KEY, startMillis)
                .build()

        val notificationWork = OneTimeWorkRequest.Builder(workerClass)
            .setInitialDelay(notificationDelayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("notification_work_${type.name}")
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "notification-${type.name}",
            ExistingWorkPolicy.REPLACE,
            notificationWork
        )
    }

    fun scheduleNotifications(startMillis: Long, fastingGoalId: String) {
        cancelAllNotifications()
        val endMillis = PredefinedFastingGoals.getGoalById(fastingGoalId).durationMillis
        val completeFastNotificationDelay =
            startMillis + endMillis - System.currentTimeMillis()
        // We show a notification 1 hour before the goal
        val almostCompleteFastNotificationDelay =
            completeFastNotificationDelay - TimeUnit.HOURS.toMillis(1)
        scheduleNotification(
            startMillis,
            almostCompleteFastNotificationDelay.coerceAtLeast(0),
            NotificationType.ONE_HOUR_BEFORE
        )
        scheduleNotification(
            startMillis,
            completeFastNotificationDelay.coerceAtLeast(0),
            NotificationType.COMPLETION
        )
    }

    fun cancelAllNotifications() {
        WorkManager.getInstance(context).cancelAllWork()
    }

    /**
     * Schedule a smart reminder notification at a specific time.
     * This notification reminds users to start their fast based on historical patterns or settings.
     */
    fun scheduleSmartReminder(triggerTimeMillis: Long, fastingGoalId: String) {
        val delay = triggerTimeMillis - System.currentTimeMillis()
        
        if (delay <= 0) {
            Log.w(AppConstants.LOG_TAG, "Smart reminder time is in the past, skipping scheduling")
            return
        }
        
        val inputData = Data.Builder()
            .putString(NOTIFICATION_TYPE_KEY, com.charliesbot.shared.core.models.NotificationType.SMART_REMINDER.name)
            .putLong(NOTIFICATION_FASTING_START_MILLIS_KEY, triggerTimeMillis)
            .build()
        
        val notificationWork = OneTimeWorkRequest.Builder(workerClass)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("smart_reminder")
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            "smart-reminder-notification",
            ExistingWorkPolicy.REPLACE,
            notificationWork
        )
        
        Log.d(AppConstants.LOG_TAG, "Smart reminder scheduled for ${delay / 1000} seconds from now")
    }

    /**
     * Schedule an eating window closing notification.
     * This reminds users that they should stop eating soon to prepare for fasting.
     */
    fun scheduleEatingWindowClosing(triggerTimeMillis: Long, fastingGoalId: String) {
        val delay = triggerTimeMillis - System.currentTimeMillis()
        
        if (delay <= 0) {
            Log.w(AppConstants.LOG_TAG, "Eating window closing time is in the past, skipping scheduling")
            return
        }
        
        val inputData = Data.Builder()
            .putString(NOTIFICATION_TYPE_KEY, com.charliesbot.shared.core.models.NotificationType.EATING_WINDOW_CLOSING.name)
            .putLong(NOTIFICATION_FASTING_START_MILLIS_KEY, triggerTimeMillis)
            .build()
        
        val notificationWork = OneTimeWorkRequest.Builder(workerClass)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag("eating_window_closing")
            .build()
        
        WorkManager.getInstance(context).enqueueUniqueWork(
            "eating-window-closing-notification",
            ExistingWorkPolicy.REPLACE,
            notificationWork
        )
        
        Log.d(AppConstants.LOG_TAG, "Eating window closing notification scheduled for ${delay / 1000} seconds from now")
    }

}