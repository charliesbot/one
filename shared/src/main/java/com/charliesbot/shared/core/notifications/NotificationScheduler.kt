package com.charliesbot.shared.core.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.charliesbot.shared.core.constants.NotificationConstants.NOTIFICATION_FASTING_START_MILLIS_KEY
import com.charliesbot.shared.core.constants.NotificationConstants.NOTIFICATION_TYPE_KEY
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

    fun scheduleNotifications(startMillis: Long) {
        cancelAllNotifications()
        val almostCompleteFastNotificationDelay =
            (startMillis + TimeUnit.HOURS.toMillis(15)) - System.currentTimeMillis()
        val completeFastNotificationDelay =
            (startMillis + TimeUnit.HOURS.toMillis(16)) - System.currentTimeMillis()
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

}