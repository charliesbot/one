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
import com.charliesbot.one.receivers.OpenWatchAppReceiver

class NotificationWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val notificationTypeEnum = inputData.getString(NOTIFICATION_TYPE_KEY) ?: ""
        val notificationType = NotificationType.valueOf(notificationTypeEnum)

        if (ActivityCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            with(NotificationManagerCompat.from(applicationContext)) {
                notify(NOTIFICATION_ID, createNotification(notificationType))
            }
        }

        return Result.success()
    }

    private fun createWatchNotificationAction(): NotificationCompat.Action {
        // Create an intent for the BroadcastReceiver
        val actionIntent = Intent(applicationContext, OpenWatchAppReceiver::class.java).apply {
            action = OpenWatchAppReceiver.ACTION_OPEN_WEAR_APP
            putExtra(NOTIFICATION_TYPE_KEY, inputData.getString(NOTIFICATION_TYPE_KEY))
        }

        val actionPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            0,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "Open on Watch",
            actionPendingIntent
        ).build()
    }

    private fun createNotification(notificationType: NotificationType): Notification {
        var title = ""
        var message = ""

        when (notificationType) {
            NotificationType.ONE_HOUR_BEFORE -> {
                title = "1 Hour Remaining!"
                message = "You're doing great! Just one more hour until your fast is complete."
            }

            NotificationType.COMPLETION -> {
                title = "Fasting Completed!"
                message = "Congratulations! You have completed your fast."
            }
        }

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
        wearableExtender
            .setHintContentIntentLaunchesActivity(true)
            .addAction(createWatchNotificationAction())

        return NotificationCompat.Builder(applicationContext, NotificationUtil.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(mobilePendingIntent)
            .extend(wearableExtender)
            .build()
    }

    enum class NotificationType {
        ONE_HOUR_BEFORE,
        COMPLETION
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_TYPE_KEY = "notification_type"
    }
}