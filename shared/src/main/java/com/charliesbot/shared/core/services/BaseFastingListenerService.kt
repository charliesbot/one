package com.charliesbot.shared.core.services

import android.util.Log
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.getLatestFastingState
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent

/**
 * An abstract `WearableListenerService` responsible for handling the synchronization of fasting
 * state data from remote devices (e.g., phone-to-watch or watch-to-phone).
 *
 * The processing flow for an incoming remote event is as follows:
 * 1. A data change is received in `onDataChanged`.
 * 2. The event is verified to be from a remote node via `isFromRemoteDevice`.
 * 3. The local repository is updated with the new data.
 * 4. `onPlatformFastingStateSynced()` is called for UI refreshes (like complications).
 * 5. The core business logic is delegated to the central [FastingEventManager], which in turn
 * calls the `onPlatformFastingStarted/Completed` hooks for state transition actions.
 *
 * **ARCHITECTURAL NOTE**: This service is intentionally designed to **ONLY** process remote events.
 * All fasting state changes initiated by the user on the local device should be handled by
 * the [com.charliesbot.shared.core.domain.usecase.FastingUseCase] to ensure consistent logic and immediate UI feedback. This clear
 * separation of concerns is critical to the app's architecture.
 *
 * @see FastingEventManager
 * @see com.charliesbot.shared.core.domain.usecase.FastingUseCase
 * @see FastingEventCallbacks
 */
abstract class BaseFastingListenerService : WearableListenerService(), KoinComponent,
    FastingEventCallbacks {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    protected val eventManager: FastingEventManager by inject()
    protected val fastingRepository: FastingDataRepository by inject()
    private val nodeClient: NodeClient by inject()
    private var currentLastTimestamp = 0L
    private var localNodeId: String? = null

    protected open suspend fun onPlatformFastingStarted(fastingDataItem: FastingDataItem) {
        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName}: onPlatformFastingStarted called, but no implementation."
        )
    }

    protected open suspend fun onPlatformFastingCompleted(fastingDataItem: FastingDataItem) {
        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName}: onPlatformFastingCompleted called, but no implementation."
        )
    }

    /**
     * Called after the local fasting data has been successfully updated
     * with new information received from a wearable device.
     * Subclasses can override this to perform platform-specific actions,
     * such as updating UI elements (e.g., widgets on the phone).
     */
    protected open suspend fun onPlatformFastingStateSynced() {
        // Default implementation does nothing.
        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName} - onPlatformFastingStateSynced called (no-op by default)"
        )
    }

    override suspend fun onFastingStarted(fastingDataItem: FastingDataItem) {
        onPlatformFastingStarted(fastingDataItem)
    }

    override suspend fun onFastingCompleted(fastingDataItem: FastingDataItem) {
        onPlatformFastingCompleted(fastingDataItem)
    }


    private suspend fun getLocalNodeId(): String? {
        return try {
            val localNode = nodeClient.localNode.await()
            localNode.id
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to get local node ID", e)
            null
        }
    }

    private fun isFromRemoteDevice(dataEvents: DataEventBuffer): Boolean {
        if (localNodeId == null) {
            Log.w(LOG_TAG, "Local node ID not available, cannot filter local events")
            return true // Process all events if we can't determine the source
        }

        // Check if any event in the buffer is from a remote device
        for (event in dataEvents) {
            val sourceNodeId = event.dataItem.uri.host
            if (sourceNodeId != null && sourceNodeId != localNodeId) {
                Log.d(LOG_TAG, "Found remote event from node: $sourceNodeId")
                return true
            }
        }
        return false
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName} init"
        )
        serviceScope.launch {
            localNodeId = getLocalNodeId()
            currentLastTimestamp = fastingRepository.getCurrentFasting()
                ?.updateTimestamp ?: currentLastTimestamp
            Log.d(
                LOG_TAG,
                "${this::class.java.simpleName} created. Initial lastTimestamp: $currentLastTimestamp"
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(LOG_TAG, "${this::class.java.simpleName} destroyed")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)

        // This service should only process data from remote nodes.
        if (!isFromRemoteDevice(dataEvents)) {
            Log.d(
                LOG_TAG,
                "${this::class.java.simpleName} - Ignoring local data change event (localNodeId: $localNodeId)"
            )
            dataEvents.release()
            return
        }

        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName} onDataChanged from REMOTE device. Comparing against lastTimestamp: $currentLastTimestamp"
        )

        var newestRemoteItem: FastingDataItem? = null
        try {
            newestRemoteItem =
                getLatestFastingState(dataEvents)
            if (newestRemoteItem == null) {
                Log.d(LOG_TAG, "No relevant fasting state change found in this buffer.")
                return
            }

            // if most recent remote item is newer than local item, update local item
            if (newestRemoteItem.updateTimestamp > currentLastTimestamp) {
                Log.i(
                    LOG_TAG,
                    "Processing REMOTE event (Timestamp: ${newestRemoteItem.updateTimestamp} > $currentLastTimestamp)"
                )

                currentLastTimestamp = newestRemoteItem.updateTimestamp

                // Update local repository from remote source
                serviceScope.launch {
                    try {
                        fastingRepository.updateFastingStatusFromRemote(
                            startTimeInMillis = newestRemoteItem.startTimeInMillis,
                            fastingGoalId = newestRemoteItem.fastingGoalId,
                            isFasting = newestRemoteItem.isFasting,
                            lastUpdateTimestamp = newestRemoteItem.updateTimestamp
                        )
                        onPlatformFastingStateSynced()
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "Failed to update repository from remote", e)
                        throw e
                    }
                }

                // Delegate all state change logic to the central manager
                serviceScope.launch {
                    try {
                        eventManager.processStateChange(
                            newestRemoteItem,
                            this@BaseFastingListenerService
                        )
                        Log.d(
                            LOG_TAG,
                            "Successfully processed remote state change via EventManager"
                        )
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "Failed to process remote state change via EventManager", e)
                    }
                }
            } else {
                Log.d(
                    LOG_TAG,
                    "Remote event timestamp ${newestRemoteItem.updateTimestamp} is not newer than current $currentLastTimestamp, skipping"
                )
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "onDataChanged couldn't process data: $e")
        } finally {
            dataEvents.release()
        }
    }
}
