package com.charliesbot.shared.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager

object NotificationUtil {
    const val CHANNEL_ID = "one_fasting_notification_channel"

    fun createNotificationChannel(context: Context) {
        val name = "One Fasting Notifications"
        val ringtoneManager = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        // Add audio attributes
        // first we set usage; "why" you are playing a sound, what is this sound used for.
        // second we set the content type; "what" you are playing.
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for your fasting schedule"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            setSound(ringtoneManager, audioAttributes)
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}