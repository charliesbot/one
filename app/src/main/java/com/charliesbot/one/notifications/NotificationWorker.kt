package com.charliesbot.one.notifications

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.charliesbot.one.MainActivity
import com.charliesbot.one.R
import com.charliesbot.shared.core.notifications.NotificationUtil
import com.charliesbot.shared.core.constants.NotificationConstants.NOTIFICATION_ID
import com.charliesbot.shared.core.models.NotificationWorkerInput
import com.charliesbot.shared.core.utils.generateDismissalId
import com.charliesbot.shared.core.utils.getNotificationText
import com.charliesbot.shared.core.utils.parseWorkerInput

class NotificationWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        if (ActivityCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            with(NotificationManagerCompat.from(applicationContext)) {
                notify(NOTIFICATION_ID, createNotification(parseWorkerInput(inputData)))
            }
        }

        return Result.success()
    }

    private fun createNotification(
        notificationWorkerInput: NotificationWorkerInput
    ): Notification {
        val notificationContent = getNotificationText(notificationWorkerInput.notificationType)

        val mobileIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val mobilePendingIntent =
            PendingIntent.getActivity(
                applicationContext,
                0,
                mobileIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

        val wearableExtender = NotificationCompat.WearableExtender()
            .setHintContentIntentLaunchesActivity(true)
            .setDismissalId(
                generateDismissalId(
                    notificationWorkerInput.fastingStartMillis,
                    notificationWorkerInput.notificationType
                )
            )

        return NotificationCompat.Builder(applicationContext, NotificationUtil.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_status)
            .setAutoCancel(true)
            .setContentTitle(notificationContent.title)
            .setContentText(notificationContent.message)
            .setContentIntent(mobilePendingIntent)
            .extend(wearableExtender)
            .build()
    }
}