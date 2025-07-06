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
import com.charliesbot.onewearos.presentation.MainActivity
import com.charliesbot.onewearos.R
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.constants.NotificationConstants
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.notifications.NotificationUtil
import com.charliesbot.shared.core.utils.FastingProgress
import com.charliesbot.shared.core.utils.FastingProgressUtil
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class OngoingActivityManager(
    private val context: Context
) {
    private val TAG = "OngoingActivityManager"
    private var updateJob: Job? = null
    private var isActive = false

    // Use Application scope to survive service restarts
    private val updateScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineName("OngoingActivityUpdater")
    )

    companion object {
        private const val ONGOING_ACTIVITY_LOCUS_ID = "ongoing_fasting_activity"
        private const val UPDATE_INTERVAL_MINUTES = 15L
    }

    private fun getNotificationProgressText(
        fastingProgress: FastingProgress,
        fastingGoal: FastGoal
    ): String {
        return if (fastingProgress.isComplete) {
            context.getString(R.string.notification_completion_title)
        } else {
            context.getString(
                R.string.complication_text_fasting_format,
                fastingProgress.progressPercentage,
                fastingProgress.elapsedHours.toInt(),
                context.getString(
                    R.string.target_duration_short,
                    fastingGoal.durationDisplay
                )
            )
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun createOngoingActivity(fastingDataItem: FastingDataItem) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate current progress
        val fastingGoal = PredefinedFastingGoals.getGoalById(fastingDataItem.fastingGoalId)
        val progress = FastingProgressUtil.calculateFastingProgress(fastingDataItem)
        val statusText = getNotificationProgressText(progress, fastingGoal)

        Log.d(TAG, "Creating ongoing activity with progress: ${progress.progressPercentage}%")

        val notificationBuilder = NotificationCompat.Builder(context, NotificationUtil.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_status)
            .setContentTitle("Fasting Timer")
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setSilent(true)
            .setLocusId(LocusIdCompat(ONGOING_ACTIVITY_LOCUS_ID))

        val ongoingActivityStatus = Status.Builder()
            .addTemplate(statusText)
            .build()

        val ongoingActivity = OngoingActivity.Builder(
            context,
            NotificationConstants.ONGOING_NOTIFICATION_ID,
            notificationBuilder
        )
            .setAnimatedIcon(R.drawable.ic_notification_status)
            .setStaticIcon(R.drawable.ic_notification_status)
            .setTouchIntent(pendingIntent)
            .setStatus(ongoingActivityStatus)
            .setLocusId(LocusIdCompat(ONGOING_ACTIVITY_LOCUS_ID))
            .build()

        // Apply the ongoing activity BEFORE notifying
        ongoingActivity.apply(context)

        // Then notify
        NotificationManagerCompat.from(context)
            .notify(NotificationConstants.ONGOING_NOTIFICATION_ID, notificationBuilder.build())
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun startOngoingActivity(fastingDataItem: FastingDataItem) {
        Log.d(TAG, "Starting ongoing activity for fasting goal: ${fastingDataItem.fastingGoalId}")

        // Stop any existing update job
        stopPeriodicUpdates()

        // Create the initial ongoing activity
        createOngoingActivity(fastingDataItem)

        // Start periodic updates
        startPeriodicUpdates(fastingDataItem)

        isActive = true
    }

    fun stopOngoingActivity() {
        Log.d(TAG, "Stopping ongoing activity")

        isActive = false

        // Stop periodic updates
        stopPeriodicUpdates()

        // Cancel the ongoing notification
        NotificationManagerCompat.from(context)
            .cancel(NotificationConstants.ONGOING_NOTIFICATION_ID)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun updateOngoingActivity(fastingDataItem: FastingDataItem) {
        if (isActive) {
            Log.d(TAG, "Manually updating ongoing activity")
            // Recreate the ongoing activity with fresh data - this is Google's recommended approach
            createOngoingActivity(fastingDataItem)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun startPeriodicUpdates(originalFastingDataItem: FastingDataItem) {
        Log.d(TAG, "Starting periodic updates every $UPDATE_INTERVAL_MINUTES minute(s)")

        updateJob = updateScope.launch {
            try {
                while (isActive && currentCoroutineContext().isActive) {
                    delay(TimeUnit.MINUTES.toMillis(UPDATE_INTERVAL_MINUTES))

                    if (isActive && currentCoroutineContext().isActive) {
                        Log.d(TAG, "Performing periodic update")

                        // Create fresh FastingDataItem with current timestamp for accurate progress calculation
                        val freshFastingDataItem = originalFastingDataItem.copy(
                            updateTimestamp = System.currentTimeMillis()
                        )

                        // Recreate the ongoing activity with fresh progress
                        withContext(Dispatchers.Main) {
                            createOngoingActivity(freshFastingDataItem)
                        }
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Periodic updates cancelled (this is normal when stopping)")
                throw e // Re-throw to properly handle cancellation
            } catch (e: Exception) {
                Log.e(TAG, "Error in periodic updates", e)
                // Don't re-throw other exceptions, just log them
            }
        }
    }

    private fun stopPeriodicUpdates() {
        updateJob?.let { job ->
            if (job.isActive) {
                Log.d(TAG, "Stopping periodic updates")
                job.cancel()
            }
        }
        updateJob = null
    }

    fun isCurrentlyActive(): Boolean = isActive

    // Call this when the app is destroyed to clean up resources
    fun cleanup() {
        Log.d(TAG, "Cleaning up OngoingActivityManager")
        stopOngoingActivity()
        updateScope.cancel()
    }
}