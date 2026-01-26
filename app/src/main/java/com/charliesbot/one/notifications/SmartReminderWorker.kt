package com.charliesbot.one.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.DataLayerConstants
import com.charliesbot.shared.core.data.repositories.settingsRepository.SettingsRepository
import com.charliesbot.shared.core.domain.usecase.GetSuggestedFastingStartTimeUseCase
import com.charliesbot.shared.core.notifications.NotificationScheduler
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * A daily worker that calculates the suggested fasting start time and:
 * 1. Schedules local smart reminder notifications on the phone
 * 2. Syncs the suggested time to the watch via Wearable Data Layer
 *
 * This worker runs once daily, typically in the morning, to set up reminders for the day.
 */
class SmartReminderWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters), KoinComponent {

    private val getSuggestedFastingStartTimeUseCase: GetSuggestedFastingStartTimeUseCase by inject()
    private val notificationScheduler: NotificationScheduler by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val dataClient: DataClient by inject()

    override suspend fun doWork(): Result {
        Log.d(LOG_TAG, "SmartReminderWorker: Starting daily smart reminder calculation")

        // Check if smart reminders are enabled
        val smartRemindersEnabled = settingsRepository.smartRemindersEnabled.first()
        if (!smartRemindersEnabled) {
            Log.d(LOG_TAG, "SmartReminderWorker: Smart reminders disabled, skipping")
            return Result.success()
        }

        return try {
            // 1. Calculate the suggested start time
            val suggestion = getSuggestedFastingStartTimeUseCase.execute()
            Log.d(LOG_TAG, "SmartReminderWorker: Suggested time calculated - ${suggestion.suggestedTimeMinutes} minutes, reason: ${suggestion.reasoning}")

            // 2. Schedule local notifications on the phone
            notificationScheduler.scheduleSmartReminderNotifications(suggestion.suggestedTimeMillis)

            // 3. Sync to Wear OS
            syncToWearOS(suggestion.suggestedTimeMillis, suggestion.reasoning)

            Log.d(LOG_TAG, "SmartReminderWorker: Completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "SmartReminderWorker: Failed to calculate/schedule smart reminders", e)
            Result.retry()
        }
    }

    private suspend fun syncToWearOS(suggestedTimeMillis: Long, reasoning: String) {
        try {
            val request = PutDataMapRequest.create(DataLayerConstants.SMART_REMINDER_PATH).apply {
                dataMap.putLong(DataLayerConstants.SMART_REMINDER_SUGGESTED_TIME_KEY, suggestedTimeMillis)
                dataMap.putString(DataLayerConstants.SMART_REMINDER_REASONING_KEY, reasoning)
                dataMap.putLong(DataLayerConstants.SMART_REMINDER_TIMESTAMP_KEY, System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(LOG_TAG, "SmartReminderWorker: Synced smart reminder to watch")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "SmartReminderWorker: Failed to sync to watch", e)
            // Don't fail the whole worker just because sync failed
        }
    }

    companion object {
        private const val WORK_NAME = "smart_reminder_daily_worker"

        /**
         * Schedule the daily smart reminder worker.
         * Runs every 24 hours, starting approximately at the next occurrence of the target hour.
         */
        fun scheduleDailyWorker(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<SmartReminderWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.d(LOG_TAG, "SmartReminderWorker: Daily worker scheduled")
        }

        /**
         * Cancel the daily smart reminder worker.
         */
        fun cancelDailyWorker(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(LOG_TAG, "SmartReminderWorker: Daily worker cancelled")
        }

        /**
         * Trigger an immediate run of the smart reminder calculation.
         * Useful when settings change or user manually requests a refresh.
         */
        fun triggerImmediateRun(context: Context) {
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<SmartReminderWorker>()
                .addTag("${WORK_NAME}_immediate")
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(LOG_TAG, "SmartReminderWorker: Immediate run triggered")
        }
    }
}

