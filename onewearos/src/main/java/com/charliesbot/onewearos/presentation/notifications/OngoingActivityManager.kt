package com.charliesbot.onewearos.presentation.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.LocusIdCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.charliesbot.onewearos.R
import com.charliesbot.onewearos.presentation.MainActivity
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.NotificationConstants
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.notifications.NotificationUtil
import com.charliesbot.shared.core.utils.FastingProgress
import com.charliesbot.shared.core.utils.FastingProgressUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OngoingActivityManager(
    private val context: Context,
    private val fastingDataRepository: FastingDataRepository
) {
    private var periodicUpdateJob: Job? = null
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun startOngoingActivity() {
        Log.d(LOG_TAG, "OngoingActivityManager: Starting ongoing activity.")
        managerScope.launch {
            updateNotification()
        }
        startPeriodicUpdates()
    }

    fun stopOngoingActivity() {
        Log.d(LOG_TAG, "OngoingActivityManager: Stopping ongoing activity.")
        periodicUpdateJob?.cancel()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun requestUpdate() {
        Log.d(LOG_TAG, "OngoingActivityManager: Requesting immediate update.")
        managerScope.launch {
            updateNotification()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun startPeriodicUpdates() {
        if (periodicUpdateJob?.isActive == true) {
            Log.d(LOG_TAG, "OngoingActivityManager: Periodic updates already running.")
            return
        }
        periodicUpdateJob = managerScope.launch {
            while (isActive) {
                delay(15_000) // 15-second interval
                updateNotification()
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private suspend fun updateNotification() {
        val data = fastingDataRepository.fastingDataItem.first()
        val progress = FastingProgressUtil.calculateFastingProgress(data)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationBuilder = buildNotification(data, progress, pendingIntent)
        val status = Status.Builder()
            .addTemplate(createStatusText(data, progress))
            .build()

        val ongoingActivity = OngoingActivity.Builder(
            context,
            NotificationConstants.ONGOING_NOTIFICATION_ID,
            notificationBuilder
        )

            .setAnimatedIcon(R.drawable.ic_notification_status)
            .setStaticIcon(R.drawable.ic_notification_status)
            .setTouchIntent(pendingIntent)
            .setStatus(status)
            .setLocusId(LocusIdCompat(ONGOING_ACTIVITY_LOCUS_ID))
            .build()

        ongoingActivity.apply(context)
        NotificationManagerCompat.from(context)
            .notify(NotificationConstants.ONGOING_NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createStatusText(
        fastingDataItem: FastingDataItem,
        fastingProgress: FastingProgress
    ): String {
        val fastingGoal = PredefinedFastingGoals.getGoalById(fastingDataItem.fastingGoalId)
        return if (fastingProgress.isComplete) {
            context.getString(com.charliesbot.shared.R.string.notification_completion_title)
        } else {
            context.getString(
                R.string.complication_text_fasting_format,
                fastingProgress.progressPercentage,
                fastingProgress.elapsedHours.toString(),
                context.getString(
                    R.string.target_duration_short,
                    fastingGoal.durationDisplay
                )
            )
        }
    }


    private fun buildNotification(
        fastDataItem: FastingDataItem,
        progress: FastingProgress,
        pendingIntent: PendingIntent,
    ): NotificationCompat.Builder {
        val notificationBuilder = NotificationCompat.Builder(context, NotificationUtil.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_status)
            .setContentTitle("Fasting Timer")
            .setContentText(createStatusText(fastDataItem, progress))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setSilent(true)
            .setLocusId(LocusIdCompat(ONGOING_ACTIVITY_LOCUS_ID))
        return notificationBuilder
    }

    companion object {
        private const val ONGOING_ACTIVITY_LOCUS_ID = "ongoing_fasting_activity"
    }
}