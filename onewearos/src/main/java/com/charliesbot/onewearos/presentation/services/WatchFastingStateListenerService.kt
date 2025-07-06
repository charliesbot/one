package com.charliesbot.onewearos.presentation.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.charliesbot.onewearos.R
import com.charliesbot.onewearos.complication.ComplicationUpdateManager
import com.charliesbot.onewearos.presentation.notifications.OngoingActivityManager
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.services.BaseFastingListenerService
import kotlinx.coroutines.flow.first
import org.koin.core.component.inject

class WatchFastingStateListenerService : BaseFastingListenerService() {
    private val complicationUpdateManager: ComplicationUpdateManager by inject()
    private val ongoingActivityManager: OngoingActivityManager by inject()
    private val fastingDataRepository: FastingDataRepository by inject()

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 9999
        private const val FOREGROUND_CHANNEL_ID = "fasting_service_channel"
        private const val FOREGROUND_CHANNEL_NAME = "Fasting Service"
    }

    // This is called for general data syncs, not just start/stop
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onPlatformFastingStateSynced() {
        super.onPlatformFastingStateSynced()
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Handling a remote data sync")

        // Always update complications
        complicationUpdateManager.requestUpdate()

        // Get the latest data and update the Ongoing Activity to match
        try {
            val currentData = fastingDataRepository.fastingDataItem.first()
            if (currentData.isFasting) {
                ongoingActivityManager.updateOngoingActivity(currentData)
                Log.d(
                    LOG_TAG,
                    "${this::class.java.simpleName} - Updated ongoing activity with fresh synced data"
                )
            } else {
                Log.d(
                    LOG_TAG,
                    "${this::class.java.simpleName} - No active fasting session, no ongoing activity update needed"
                )
            }
        } catch (e: Exception) {
            Log.e(
                LOG_TAG,
                "${this::class.java.simpleName} - Error updating ongoing activity during sync",
                e
            )
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Creating service")

        // Start as foreground service immediately when created
        // This ensures the service stays alive to manage ongoing activities
        startForegroundServiceIfNeeded()
    }

    override fun onDestroy() {
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Service being destroyed")

        // DON'T clean up ongoing activity manager here!
        // The ongoing activity should persist and be managed by the application-scoped manager

        super.onDestroy()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "${this::class.java.simpleName} - onStartCommand called")

        // Ensure we're running as foreground service
        startForegroundServiceIfNeeded()

        return START_STICKY
    }

    // Called when the PHONE starts a fast
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onPlatformFastingStarted(fastingDataItem: FastingDataItem) {
        super.onPlatformFastingStarted(fastingDataItem)
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Fast started from REMOTE")

        // Tell the manager to start the activity
        ongoingActivityManager.startOngoingActivity(fastingDataItem)
    }

    // Called when the PHONE stops a fast
    override suspend fun onPlatformFastingCompleted(fastingDataItem: FastingDataItem) {
        super.onPlatformFastingCompleted(fastingDataItem)
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Fast completed from REMOTE")

        // Tell the manager to stop the activity
        ongoingActivityManager.stopOngoingActivity()

        // Stop the foreground service since it's no longer needed
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun startForegroundServiceIfNeeded() {
        // Only start foreground service if not already running
        try {
            createNotificationChannel()
            val notification = createForegroundNotification()

            ServiceCompat.startForeground(
                this,
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )

            Log.d(LOG_TAG, "${this::class.java.simpleName} - Started as foreground service")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "${this::class.java.simpleName} - Failed to start foreground service", e)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            FOREGROUND_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
            .apply {
                description = "Background service for fasting tracking"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setContentTitle("Fasting Tracker")
            .setContentText("Tracking your fasting session")
            .setSmallIcon(R.drawable.ic_notification_status)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .build()
    }
}