package com.charliesbot.onewearos.presentation.notifications

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
import com.charliesbot.onewearos.R
import com.charliesbot.onewearos.presentation.MainActivity
import com.charliesbot.shared.core.abstraction.StringProvider
import com.charliesbot.shared.core.constants.NotificationConstants.NOTIFICATION_ID
import com.charliesbot.shared.core.data.repositories.preferencesRepository.PreferencesRepository
import com.charliesbot.shared.core.models.NotificationWorkerInput
import com.charliesbot.shared.core.notifications.NotificationUtil
import com.charliesbot.shared.core.utils.generateDismissalId
import com.charliesbot.shared.core.utils.getNotificationText
import com.charliesbot.shared.core.utils.parseWorkerInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters), KoinComponent {

    private val stringProvider: StringProvider by inject()
    private val preferencesRepository: PreferencesRepository by inject()

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
        val notificationContent =
            getNotificationText(notificationWorkerInput.notificationType, stringProvider)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val watchPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get vibration setting from preferences
        val vibrationEnabled = runBlocking {
            preferencesRepository.getVibrationEnabled().first()
        }

        val wearableExtender = NotificationCompat.WearableExtender()
            .setHintContentIntentLaunchesActivity(true)
            .setDismissalId(
                generateDismissalId(
                    notificationWorkerInput.fastingStartMillis,
                    notificationWorkerInput.notificationType
                )
            )

        val notificationBuilder =
            NotificationCompat.Builder(applicationContext, NotificationUtil.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_status)
                .setContentTitle(notificationContent.title)
                .setContentText(notificationContent.message)
                .setContentIntent(watchPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .extend(wearableExtender)

        // Add vibration pattern only if enabled
        if (vibrationEnabled) {
            notificationBuilder.setVibrate(longArrayOf(0, 250, 100, 250))
        }

        return notificationBuilder.build()
    }
}