package com.charliesbot.shared.core.services

import android.util.Log
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.notifications.NotificationScheduler
import com.charliesbot.shared.core.utils.getLatestFastingState
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.android.ext.android.inject

abstract class BaseFastingListenerService : WearableListenerService(), KoinComponent {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    protected val notificationScheduler: NotificationScheduler by inject()
    protected val fastingRepository: FastingDataRepository by inject()
    private var currentLastTimestamp = 0L

    override fun onCreate() {
        super.onCreate()
        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName} init"
        )
        serviceScope.launch {
            currentLastTimestamp = fastingRepository.getCurrentFasting()
                ?.updateTimestamp ?: currentLastTimestamp
            Log.d(
                LOG_TAG,
                "${this::class.java.simpleName} created. Initial lastTimestamp: $currentLastTimestamp"
            )
        }
    }

    /**
     * Called after the local fasting data has been successfully updated
     * with new information received from a wearable device.
     * Subclasses can override this to perform platform-specific actions,
     * such as updating UI elements (e.g., widgets on the phone).
     */
    protected open suspend fun onFastingStateSynced() {
        // Default implementation does nothing.
        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName} - Base onFastingStateSynced called (no-op by default)"
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(LOG_TAG, "${this::class.java.simpleName} destroyed")
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName} onDataChanged. Comparing against lastTimestamp: $currentLastTimestamp"
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
                    "Processing NEW event (Timestamp: ${newestRemoteItem.updateTimestamp} > $currentLastTimestamp)"
                )
                currentLastTimestamp = newestRemoteItem.updateTimestamp
                serviceScope.launch {
                    fastingRepository.updateFastingStatusFromRemote(
                        startTimeInMillis = newestRemoteItem.startTimeInMillis,
                        isFasting = newestRemoteItem.isFasting,
                        lastUpdateTimestamp = newestRemoteItem.updateTimestamp
                    )
                    onFastingStateSynced()
                }

                if (newestRemoteItem.isFasting) {
                    Log.d(
                        LOG_TAG,
                        "Service: Scheduling notifications for start: ${newestRemoteItem.startTimeInMillis}"
                    )
                    notificationScheduler.scheduleNotifications(newestRemoteItem.startTimeInMillis)
                } else {
                    Log.d(LOG_TAG, "Cancelling notifications")
                    notificationScheduler.cancelAllNotifications()
                }

            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "onDataChanged couldn't process data: $e")
        } finally {
            dataEvents.release()
        }
    }
}
