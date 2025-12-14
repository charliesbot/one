package com.charliesbot.onewearos.presentation.services

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.charliesbot.onewearos.R
import com.charliesbot.onewearos.presentation.notifications.OngoingActivityManager
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.NotificationConstants
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.notifications.NotificationUtil
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Foreground service for displaying the fasting timer as an OngoingActivity.
 *
 * Required for showing the timer in the Wear OS Recents list.
 * Uses StopwatchPart so the system auto-updates the timer - no periodic wake-ups needed.
 */
class OngoingActivityService : Service(), KoinComponent {

    private val fastingDataRepository: FastingDataRepository by inject()
    private lateinit var ongoingActivityManager: OngoingActivityManager
    private val serviceTag = this::class.java.simpleName

    override fun onCreate() {
        super.onCreate()
        ongoingActivityManager = OngoingActivityManager(this, fastingDataRepository)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_STOP -> {
                Log.d(LOG_TAG, "$serviceTag received STOP action.")
                ongoingActivityManager.stopOngoingActivity()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                Log.d(LOG_TAG, "$serviceTag received START action.")

                val startTimeMillis = intent?.getLongExtra(EXTRA_START_TIME, 0L) ?: 0L
                val fastingGoalId = intent?.getStringExtra(EXTRA_FASTING_GOAL_ID) ?: ""

                if (startTimeMillis == 0L || fastingGoalId.isEmpty()) {
                    Log.e(LOG_TAG, "$serviceTag missing required extras. startTime=$startTimeMillis, goalId=$fastingGoalId")
                    stopSelf()
                    return START_NOT_STICKY
                }

                // Fulfill the foreground service promise IMMEDIATELY
                val initialNotification = createInitialNotification()
                startForeground(
                    NotificationConstants.ONGOING_NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                )

                // Now create the real ongoing activity with auto-updating stopwatch
                ongoingActivityManager.startOngoingActivity(startTimeMillis, fastingGoalId)

                return START_NOT_STICKY  // Don't restart if killed - state may be stale
            }
            else -> {
                Log.w(LOG_TAG, "$serviceTag received unknown action: $action")
                return START_NOT_STICKY
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "$serviceTag is being destroyed.")
        ongoingActivityManager.stopOngoingActivity()
    }

    private fun createInitialNotification(): Notification {
        NotificationUtil.createNotificationChannel(this)

        return NotificationCompat.Builder(this, NotificationUtil.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_status)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.charliesbot.onewearos.ACTION_START_ONGOING"
        const val ACTION_STOP = "com.charliesbot.onewearos.ACTION_STOP_ONGOING"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_FASTING_GOAL_ID = "extra_fasting_goal_id"

        fun createStartIntent(context: Context, startTimeMillis: Long, fastingGoalId: String): Intent {
            return Intent(context, OngoingActivityService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_START_TIME, startTimeMillis)
                putExtra(EXTRA_FASTING_GOAL_ID, fastingGoalId)
            }
        }

        fun createStopIntent(context: Context): Intent {
            return Intent(context, OngoingActivityService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}