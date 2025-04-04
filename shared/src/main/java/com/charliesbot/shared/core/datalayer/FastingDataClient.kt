package com.charliesbot.shared.core.datalayer

import android.content.Context
import android.util.Log
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.DataLayerConstants
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

private enum class FetchType {
    INITIAL,
    FROM_DATA_LAYER,
    IGNORED,
    UPDATE
}

class FastingDataClient(context: Context) {
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
                Log.d(LOG_TAG, "Connected nodes: ${nodes.map { it.displayName }}")
                if (nodes.isEmpty()) {
                    Log.w(LOG_TAG, "No connected nodes found!")
                }
            }
        dataClient.addListener(dataListener)
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
                    LOG_TAG,
                    "Retrieved existing data: isFasting=$isFasting, startTime=$startTimeInMillis, timestamp=$updateTimestamp"
                )

            FetchType.FROM_DATA_LAYER -> Log.d(
                LOG_TAG,
                "State updated from Data Layer: isFasting=$isFasting, startTime=$startTimeInMillis, timestamp=$updateTimestamp"
            )

            FetchType.IGNORED -> Log.d(
                LOG_TAG,
                "Ignoring outdated Data Layer event with timestamp: $updateTimestamp, last processed: $lastUpdateTimestamp"
            )

            FetchType.UPDATE -> Log.d(
                LOG_TAG,
                "Updating state: isFasting=$isFasting, startTime=$startTimeInMillis, timestamp=$updateTimestamp"
            )
        }
    }

    private fun handleDataEvents(dataEvents: DataEventBuffer) {
        Log.d(LOG_TAG, "Data changed event received - count: ${dataEvents.count}")
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == DataLayerConstants.FASTING_PATH) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                updateLocalState(dataMap, FetchType.FROM_DATA_LAYER)
            }
        }
    }

    private fun updateLocalState(dataMap: DataMap, fetchType: FetchType) {
        val isFasting = dataMap.getBoolean(DataLayerConstants.IS_FASTING_KEY, false)
        val startTime = dataMap.getLong(DataLayerConstants.START_TIME_KEY, 0L)
        val updateTimestamp = dataMap.getLong(DataLayerConstants.UPDATE_TIMESTAMP_KEY, 0L)

        if (fetchType == FetchType.FROM_DATA_LAYER && updateTimestamp <= lastUpdateTimestamp) {
            logBasedOnStateData(FetchType.IGNORED, isFasting, startTime, updateTimestamp)
            return
        }

        lastUpdateTimestamp = updateTimestamp
        _isFasting.value = isFasting
        _startTimeInMillis.value = startTime

        logBasedOnStateData(fetchType, isFasting, startTime, updateTimestamp)
    }

    private suspend fun updateFastingState(isFasting: Boolean, startTimeInMillis: Long) {
        val updateTimestamp = System.currentTimeMillis()
        val request: PutDataRequest =
            PutDataMapRequest.create(DataLayerConstants.FASTING_PATH).apply {
                dataMap.putBoolean(DataLayerConstants.IS_FASTING_KEY, isFasting)
                dataMap.putLong(DataLayerConstants.START_TIME_KEY, startTimeInMillis)
                dataMap.putLong(DataLayerConstants.UPDATE_TIMESTAMP_KEY, updateTimestamp)
            }.asPutDataRequest().setUrgent()

        try {
            val result: DataItem = dataClient.putDataItem(request).await()
            val dataMap = DataMapItem.fromDataItem(result).dataMap
            updateLocalState(dataMap, FetchType.UPDATE)
            Log.d(
                LOG_TAG,
                "State updated in Data Layer: isFasting=$isFasting, startTime=$startTimeInMillis"
            )
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error updating fasting state in Data Layer", e)
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
        Log.d(LOG_TAG, "Data listener removed")
    }
}