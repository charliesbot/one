package com.charliesbot.onewearos.presentation.data.repositories

import android.content.Context
import android.util.Log
import com.charliesbot.shared.core.models.CommandStatus
import com.charliesbot.shared.core.models.FastingCommand
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

fun Long.toByteArray(): ByteArray =
    ByteBuffer.allocate(Long.SIZE_BYTES).putLong(this).array()

class WearMessageRepository(private val context: Context) {
    private val messageClient by lazy { Wearable.getMessageClient(context) }

    suspend fun sendCommandToMobile(
        command: FastingCommand,
        startTimeInMillis: Long? = null
    ): CommandStatus {
        return withContext(Dispatchers.IO) {
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                var error = ""
                if (nodes.isEmpty()) {
                    error = "No connected nodes found"
                    Log.e("FastingDataClient - Watch App", error)
                    return@withContext CommandStatus.Error(error)
                }
                // Prefer nearby nodes, or fall back to the first available node
                val targetNode = nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()

                if (targetNode == null) {
                    error = "Could not find a suitable node to send message to"
                    Log.w("FastingDataClient - Watch App", error)
                    return@withContext CommandStatus.Error(error)
                }

                val messageTask =
                    messageClient.sendMessage(
                        targetNode.id,
                        command.path,
                        startTimeInMillis?.toByteArray()
                    )
                Tasks.await(messageTask)
                Log.d(
                    "FastingDataClient - Watch App",
                    "Sent ${command.name} to ${targetNode.displayName}"
                )
                return@withContext CommandStatus.Success
            } catch (e: Exception) {
                val error = "Error sending command: ${e.message}"
                Log.e("FastingDataClient - Watch App", error)
                return@withContext CommandStatus.Error(error)
            }
        }
    }
}
