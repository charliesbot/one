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
import com.charliesbot.shared.core.notifications.NotificationUtil
import android.os.SystemClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Manages the OngoingActivity for fasting sessions on Wear OS.
 *
 * Uses Status.StopwatchPart for auto-updating elapsed time display.
 * The system handles ticking the timer automatically - no periodic updates needed.
 */
class OngoingActivityManager(
    private val context: Context,
    private val fastingDataRepository: FastingDataRepository
) {

    /**
     * Starts the ongoing activity with a stopwatch that auto-updates.
     * @param startTimeMillis The timestamp when the fasting session started
     * @param fastingGoalId The ID of the fasting goal
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun startOngoingActivity(startTimeMillis: Long, fastingGoalId: String) {
        Log.d(LOG_TAG, "OngoingActivityManager: Starting ongoing activity. startTime=$startTimeMillis, goalId=$fastingGoalId")

        val fastingGoal = PredefinedFastingGoals.getGoalById(fastingGoalId)
        val pendingIntent = createTouchIntent()

        val notificationBuilder = buildNotification(startTimeMillis, fastingGoal.durationDisplay, pendingIntent)

        // StopwatchPart uses SystemClock.elapsedRealtime() base, not Unix epoch
        // Convert from System.currentTimeMillis() to elapsedRealtime base
        val elapsedSinceStart = System.currentTimeMillis() - startTimeMillis
        val stopwatchStartTime = SystemClock.elapsedRealtime() - elapsedSinceStart

        // Use StopwatchPart - system auto-updates the elapsed time, no periodic loop needed
        // Build template from localized string, replacing format args with Status placeholders
        val statusTemplate = context.getString(R.string.ongoing_status_format, "#time#", "#goal#")
        val goalText = context.getString(R.string.target_duration_short, fastingGoal.durationDisplay)
        val status = Status.Builder()
            .addTemplate(statusTemplate)
            .addPart("time", Status.StopwatchPart(stopwatchStartTime))
            .addPart("goal", Status.TextPart(goalText))
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

        Log.d(LOG_TAG, "OngoingActivityManager: Ongoing activity started successfully")
    }

    /**
     * Stops the ongoing activity and cancels the notification.
     */
    fun stopOngoingActivity() {
        Log.d(LOG_TAG, "OngoingActivityManager: Stopping ongoing activity.")
        NotificationManagerCompat.from(context).cancel(NotificationConstants.ONGOING_NOTIFICATION_ID)
        Log.d(LOG_TAG, "OngoingActivityManager: Ongoing activity stopped.")
    }

    /**
     * Updates the ongoing activity (e.g., when goal changes mid-fast).
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun requestUpdate() {
        Log.d(LOG_TAG, "OngoingActivityManager: Requesting update.")
        val data = runBlocking {
            fastingDataRepository.fastingDataItem.first()
        }
        if (data.isFasting) {
            startOngoingActivity(data.startTimeInMillis, data.fastingGoalId)
        }
    }

    private fun createTouchIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(
        startTimeMillis: Long,
        goalDuration: String,
        pendingIntent: PendingIntent,
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, NotificationUtil.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_status)
            .setContentTitle(context.getString(R.string.ongoing_activity_title))
            .setContentText(context.getString(R.string.target_duration_short, goalDuration))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(startTimeMillis)
            .setUsesChronometer(true)  // System auto-updates elapsed time
            .setSilent(true)
            .setLocusId(LocusIdCompat(ONGOING_ACTIVITY_LOCUS_ID))
    }

    companion object {
        private const val ONGOING_ACTIVITY_LOCUS_ID = "ongoing_fasting_activity"
    }
}