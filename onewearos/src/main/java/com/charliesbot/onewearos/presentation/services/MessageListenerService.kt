package com.charliesbot.onewearos.presentation.services

import android.content.Intent
import android.util.Log
import com.charliesbot.onewearos.presentation.MainActivity
import com.charliesbot.shared.core.models.FastingCommand
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.koin.core.component.KoinComponent

class MessageListenerService : WearableListenerService(), KoinComponent {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        when (messageEvent.path) {
            FastingCommand.OPEN_WATCH_APP.path -> {
                Log.d(
                    "FastingDataClient - MessageListenerService",
                    "Received Open Watch App message"
                )
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(mainIntent)
            }
        }

    }
}
