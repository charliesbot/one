package com.charliesbot.shared.core.data.repositories

import android.content.Context
import android.util.Log
import com.charliesbot.shared.core.models.CommandStatus
import com.charliesbot.shared.core.models.DeviceType
import com.charliesbot.shared.core.models.FastingCommand
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WearableMessageRepository(private val context: Context) {
    private val messageClient by lazy { Wearable.getMessageClient(context) }

    suspend fun sendCommandToMobile(
        command: FastingCommand,
        payload: ByteArray? = null
    ): CommandStatus {
        return sendCommand(DeviceType.WATCH, command, payload)
    }

    suspend fun sendCommandToWatch(
        command: FastingCommand,
        payload: ByteArray? = null
    ): CommandStatus {
        return sendCommand(DeviceType.MOBILE, command, payload)
    }

    private suspend fun sendCommand(
        deviceType: DeviceType,
        command: FastingCommand,
        payload: ByteArray? = null
    ): CommandStatus {
        return withContext(Dispatchers.IO) {
            val logTag = "FastingDataClient - $deviceType - App"
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                var error = ""
                if (nodes.isEmpty()) {
                    error = "No connected nodes found"
                    Log.e(logTag, error)
                    return@withContext CommandStatus.Error(error)
                }
                // Prefer nearby nodes, or fall back to the first available node
                val targetNode = nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()

                if (targetNode == null) {
                    error = "Could not find a suitable node to send message to"
                    Log.w(logTag, error)
                    return@withContext CommandStatus.Error(error)
                }

                val messageTask =
                    messageClient.sendMessage(
                        targetNode.id,
                        command.path,
                        payload
                    )
                Tasks.await(messageTask)
                Log.d(
                    logTag,
                    "Sent ${command.name} to ${targetNode.displayName}"
                )
                return@withContext CommandStatus.Success
            } catch (e: Exception) {
                val error = "Error sending command: ${e.message}"
                Log.e(logTag, error)
                return@withContext CommandStatus.Error(error)
            }
        }
    }
}
