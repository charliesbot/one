package com.charliesbot.onewearos.presentation.services

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.charliesbot.onewearos.R
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.charliesbot.onewearos.presentation.notifications.OngoingActivityManager
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.NotificationConstants
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.notifications.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OngoingActivityService : Service(), KoinComponent {

    private val fastingDataRepository: FastingDataRepository by inject()
    private lateinit var ongoingActivityManager: OngoingActivityManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serviceTag = this::class.java.simpleName

    override fun onCreate() {
        super.onCreate()
        // Instantiate the manager when the service is created
        ongoingActivityManager = OngoingActivityManager(this, fastingDataRepository)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "$serviceTag received start command.")

        // 🟢 Fulfill the foreground service promise IMMEDIATELY.
        val initialNotification = createInitialNotification()
        startForeground(NotificationConstants.ONGOING_NOTIFICATION_ID, initialNotification)

        ongoingActivityManager.startOngoingActivity()

        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "$serviceTag is being destroyed. Stopping activity manager.")
        ongoingActivityManager.stopOngoingActivity()
    }


    // This is just a placeholder icon to trigger the foreground service right away.
    // TODO: let's explore this solution provided by ChatGPT:
    // - Get rid of the foreground service entirely
    // - Why you usually don’t need a long-running service for a timer
    // - OngoingActivity already supports chronometers and countdowns that the system updates for you.
    // - You can create a single notification with a Status.ChronometerTemplate(...) or “time-remaining”
    // - template and let the OS tick the UI every second.
    // - When something meaningful happens (goal reached, user stops fast, watch reboots) you poke the notification
    // - once via a BroadcastReceiver, AlarmManager or an expedited WorkManager job.
    // - Those run instantly without a visible stub and respect Doze.
    private fun createInitialNotification(): Notification {
        NotificationUtil.createNotificationChannel(this)

        return NotificationCompat.Builder(this, NotificationUtil.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_status)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW) // It's just a placeholder
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}