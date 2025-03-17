package com.charliesbot.one.services

import android.util.Log
import com.charliesbot.one.notifications.NotificationScheduler
import com.charliesbot.shared.core.models.FastingCommand
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.koin.core.component.KoinComponent
import org.koin.android.ext.android.inject
import java.nio.ByteBuffer

fun ByteArray.toLong() =
    ByteBuffer.wrap(this).long

class MessageListenerService : WearableListenerService(), KoinComponent {
    private val notificationScheduler: NotificationScheduler by inject()

    override fun onCreate() {
        super.onCreate()
        Log.d("FastingDataClient", "MessageListenerService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("FastingDataClient", "MessageListenerService destroyed")
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        when (messageEvent.path) {
            FastingCommand.START_FASTING.path -> {
                Log.d(
                    "FastingDataClient - MessageListenerService",
                    "Received START fasting command from watch"
                )
                val startTimeBytes = messageEvent.data
                val startTimeInMillis = if (startTimeBytes.isNotEmpty()) startTimeBytes.toLong()
                else System.currentTimeMillis() // Use current time as fallback
                notificationScheduler.scheduleNotifications(startTimeInMillis)
            }

            FastingCommand.STOP_FASTING.path -> {
                Log.d(
                    "FastingDataClient - MessageListenerService",
                    "Received STOP fasting command from watch"
                )
                notificationScheduler.cancelAllNotifications()
            }

            FastingCommand.UPDATE_START_TIME.path -> {
                TODO("Not yet implemented")
            }

            else -> {
                Log.w(
                    "FastingDataClient - MessageListenerService",
                    "Unknown command received: ${messageEvent.path}"
                )
            }
        }
    }

}