package com.charliesbot.onewearos.presentation.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.charliesbot.onewearos.presentation.MainActivity
import com.charliesbot.onewearos.R
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.constants.NotificationConstants
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.notifications.NotificationUtil
import com.charliesbot.shared.core.utils.FastingProgress
import com.charliesbot.shared.core.utils.FastingProgressUtil

class OngoingActivityManager(
    private val context: Context
) {
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
    fun startOngoingActivity(startTimeMillis: Long, fastingGoalId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate progress for the status
        val fastingGoal = PredefinedFastingGoals.getGoalById(fastingGoalId)
        val progress = FastingProgressUtil.calculateFastingProgress(startTimeMillis, fastingGoalId)
        val statusText = getNotificationProgressText(progress, fastingGoal)

        val notificationBuilder = NotificationCompat.Builder(context, NotificationUtil.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_status)
            .setContentTitle("Fasting Timer")
            .setContentText(statusText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setSilent(true) // Don't make sound for ongoing activity

        val ongoingActivityStatus = Status.Builder()
            .addTemplate(statusText)
            .build()

        val ongoingActivity =
            OngoingActivity.Builder(
                context,
                NotificationConstants.ONGOING_NOTIFICATION_ID,
                notificationBuilder
            )
                .setStaticIcon(R.drawable.ic_notification_status)
                .setTouchIntent(pendingIntent)
                .setStatus(ongoingActivityStatus)
                .build()

        ongoingActivity.apply(context)

        NotificationManagerCompat.from(context)
            .notify(NotificationConstants.ONGOING_NOTIFICATION_ID, notificationBuilder.build())
    }

    fun stopOngoingActivity() {
        // Cancel the ongoing notification
        NotificationManagerCompat.from(context)
            .cancel(NotificationConstants.ONGOING_NOTIFICATION_ID)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun updateOngoingActivity(startTimeMillis: Long, fastingGoalId: String) {
        // Update the existing ongoing activity with new progress
        startOngoingActivity(startTimeMillis, fastingGoalId)
    }
}