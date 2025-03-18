package com.charliesbot.one.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.charliesbot.one.notifications.NotificationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import com.charliesbot.shared.core.data.repositories.WearableMessageRepository
import com.charliesbot.shared.core.models.FastingCommand
import org.koin.core.component.inject

class OpenWatchAppReceiver : BroadcastReceiver(), KoinComponent {
    companion object {
        const val ACTION_OPEN_WEAR_APP = "com.charliesbot.one.ACTION_OPEN_WEAR_APP"
    }

    private val wearableMessageRepository: WearableMessageRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_OPEN_WEAR_APP) {
            val notificationType = intent.getStringExtra(NotificationWorker.NOTIFICATION_TYPE_KEY)
            val payload = notificationType?.toByteArray() ?: ByteArray(0)
            CoroutineScope(Dispatchers.IO).launch {
                wearableMessageRepository.sendCommandToWatch(FastingCommand.OPEN_WATCH_APP, payload)
                Log.d("FastingDataClient - BroadcastReceiver", "Sent open app command to watch")
            }
        }
    }
}
