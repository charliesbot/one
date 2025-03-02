package com.charliesbot.shared.core.datalayer

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private enum class FetchType(val log: String) {
    INITIAL("INITIAL"),
    FROM_DATA_LAYER("FROM_DATA_LAYER"),
    IGNORED("IGNORED"),
    UPDATE("UPDATE")
}

class FastingDataClient(context: Context) {
    companion object {
        private const val TAG = "FastingDataClient" // Tag for logging
        private const val FASTING_PATH = "/fasting_state"
        private const val IS_FASTING_KEY = "is_fasting"
        private const val START_TIME_KEY = "start_time"
        private const val UPDATE_TIMESTAMP_KEY = "update_timestamp"
    }

    private val dataClient: DataClient = Wearable.getDataClient(context.applicationContext)
    private val dataListener = DataClient.OnDataChangedListener { dataEvents ->
        handleDataEvents(dataEvents)
    }

    private var lastUpdateTimestamp = 0L

    private val _isFasting = MutableStateFlow(false)
    val isFasting = _isFasting.asStateFlow()

    private val _startTimeInMillis = MutableStateFlow(0L)
    val startTimeInMillis = _startTimeInMillis.asStateFlow()

    init {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                Log.d(TAG, "Connected nodes: ${nodes.map { it.displayName }}")
                if (nodes.isEmpty()) {
                    Log.w(TAG, "No connected nodes found!")
                }
            }
        dataClient.addListener(dataListener)
        fetchData()
    }

    private fun logBasedOnStateData(
        fetchType: FetchType,
        isFasting: Boolean,
        startTimeInMillis: Long,
        updateTimestamp: Long
    ) {
        when (fetchType) {
            FetchType.INITIAL ->
                Log.d(
                    TAG,
                    "Retrieved existing data: isFasting=$isFasting, startTime=$startTimeInMillis, timestamp=$updateTimestamp"
                )

            FetchType.FROM_DATA_LAYER -> Log.d(
                TAG,
                "State updated from Data Layer: isFasting=$isFasting, startTime=$startTimeInMillis, timestamp=$updateTimestamp"
            )

            FetchType.IGNORED -> Log.d(
                TAG,
                "Ignoring outdated Data Layer event with timestamp: $updateTimestamp, last processed: $lastUpdateTimestamp"
            )

            FetchType.UPDATE -> Log.d(
                TAG,
                "Updating state: isFasting=$isFasting, startTime=$startTimeInMillis, timestamp=$updateTimestamp"
            )
        }
    }

    private fun handleDataEvents(dataEvents: DataEventBuffer) {
        Log.d(TAG, "Data changed event received - count: ${dataEvents.count}")
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == FASTING_PATH) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                updateLocalState(dataMap, FetchType.FROM_DATA_LAYER)
            }
        }
    }

    private fun updateLocalState(dataMap: DataMap, fetchType: FetchType) {
        val isFasting = dataMap.getBoolean(IS_FASTING_KEY, false)
        val startTime = dataMap.getLong(START_TIME_KEY, 0L)
        val updateTimestamp = dataMap.getLong(UPDATE_TIMESTAMP_KEY, 0L)

        // If we are trying to update the state with an outdated timestamp, ignore it
        if (fetchType == FetchType.FROM_DATA_LAYER && updateTimestamp <= lastUpdateTimestamp) {
            logBasedOnStateData(FetchType.IGNORED, isFasting, startTime, updateTimestamp)
            return
        }

        _isFasting.value = isFasting
        _startTimeInMillis.value = startTime
        lastUpdateTimestamp = updateTimestamp

        logBasedOnStateData(fetchType, isFasting, startTime, updateTimestamp)
    }

    private fun fetchData() {
        Log.d(TAG, "fetchData() STARTED") // Add this log at the start
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataItems = dataClient.dataItems.await()
                dataItems.forEach { dataItem ->
                    if (dataItem.uri.path == FASTING_PATH) {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        updateLocalState(dataMap, FetchType.INITIAL)
                        return@forEach // Exit after first relevant data item is found
                    }
                }
                dataItems.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching current data", e) // Logging error
            } finally {
                Log.d(
                    TAG,
                    "fetchData() COMPLETED"
                )
            }
        }
    }

    private suspend fun updateFastingState(isFasting: Boolean, startTimeInMillis: Long) {
        val updateTimestamp = System.currentTimeMillis()
        val request: PutDataRequest = PutDataMapRequest.create(FASTING_PATH).apply {
            dataMap.putBoolean(IS_FASTING_KEY, isFasting)
            dataMap.putLong(START_TIME_KEY, startTimeInMillis)
            dataMap.putLong(UPDATE_TIMESTAMP_KEY, updateTimestamp)
        }.asPutDataRequest().setUrgent()

        try {
            val result: DataItem = dataClient.putDataItem(request).await()
            val dataMap = DataMapItem.fromDataItem(result).dataMap
            updateLocalState(dataMap, FetchType.UPDATE)
            Log.d(
                TAG,
                "State updated in Data Layer: isFasting=$isFasting, startTime=$startTimeInMillis"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating fasting state in Data Layer", e)
        }
    }

    suspend fun startFasting(startTimeInMillis: Long = System.currentTimeMillis()) {
        updateFastingState(true, startTimeInMillis)
    }

    suspend fun stopFasting() {
        updateFastingState(false, 0L)
    }

    suspend fun updateStartTime(startTimeInMillis: Long) {
        updateFastingState(_isFasting.value, startTimeInMillis)
    }

    fun cleanup() {
        dataClient.removeListener(dataListener)
        Log.d(TAG, "Data listener removed")
    }
}