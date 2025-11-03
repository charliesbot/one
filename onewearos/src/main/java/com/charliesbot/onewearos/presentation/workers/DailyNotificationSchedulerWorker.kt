package com.charliesbot.onewearos.presentation.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.charliesbot.shared.core.constants.AppConstants
import com.charliesbot.shared.core.data.repositories.preferencesRepository.PreferencesRepository
import com.charliesbot.shared.core.domain.usecase.ScheduleSmartNotificationsUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Daily worker that schedules smart fasting reminder notifications.
 * 
 * This worker runs once per day to:
 * 1. Check if smart notifications are enabled
 * 2. Calculate the optimal notification time based on user's routine
 * 3. Schedule the next day's notifications
 * 
 * The worker is enqueued as a PeriodicWorkRequest with a 24-hour interval.
 */
class DailyNotificationSchedulerWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val preferencesRepository: PreferencesRepository by inject()
    private val scheduleSmartNotificationsUseCase: ScheduleSmartNotificationsUseCase by inject()

    override suspend fun doWork(): Result {
        Log.d(AppConstants.LOG_TAG, "DailyNotificationSchedulerWorker: Starting daily notification scheduling")
        
        // Check if smart notifications are enabled
        val enabled = preferencesRepository.getSmartNotificationsEnabled().first()
        
        if (!enabled) {
            Log.d(AppConstants.LOG_TAG, "DailyNotificationSchedulerWorker: Smart notifications disabled, skipping")
            return Result.success()
        }
        
        return try {
            // Schedule tomorrow's smart notifications
            scheduleSmartNotificationsUseCase()
            Log.d(AppConstants.LOG_TAG, "DailyNotificationSchedulerWorker: Successfully scheduled notifications")
            Result.success()
        } catch (e: Exception) {
            Log.e(AppConstants.LOG_TAG, "DailyNotificationSchedulerWorker: Error scheduling notifications", e)
            Result.retry()
        }
    }
}

