package com.charliesbot.one.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.charliesbot.one.notifications.NotificationWorker.NotificationType


class NotificationScheduler(private val context: Context) {
    private fun scheduleNotification(
        notificationDelayMillis: Long,
        type: NotificationType
    ) {
        val inputData =
            Data.Builder()
                .putString(NotificationWorker.NOTIFICATION_TYPE_KEY, type.name).build()

        val notificationWork = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(notificationDelayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
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
            almostCompleteFastNotificationDelay.coerceAtLeast(0),
            NotificationType.ONE_HOUR_BEFORE
        )
        scheduleNotification(
            completeFastNotificationDelay.coerceAtLeast(0),
            NotificationType.COMPLETION
        )
    }


    fun cancelAllNotifications() {
        WorkManager.getInstance(context).cancelAllWork()
    }
}