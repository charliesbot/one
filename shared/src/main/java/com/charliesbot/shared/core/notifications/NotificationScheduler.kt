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

    suspend fun scheduleNotifications(startMillis: Long, fastingGoalId: String) {
        cancelAllNotifications()
        
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

    fun cancelAllNotifications() {
        WorkManager.getInstance(context).cancelAllWork()
    }

}