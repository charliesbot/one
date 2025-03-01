package com.charliesbot.shared.core.datalayer

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
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

class FastingDataClient(context: Context) {
    companion object {
        private const val TAG = "FastingDataClient" // Tag for logging
        private const val FASTING_PATH = "/fasting_state"
        private const val IS_FASTING_KEY = "is_fasting"
        private const val START_TIME_KEY = "start_time"
    }

    private val dataClient: DataClient = Wearable.getDataClient(context.applicationContext)
    private val dataListener =
        DataClient.OnDataChangedListener { dataEvents -> // Store listener instance
            dataEvents.forEach { event ->
                if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == FASTING_PATH) {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    updateLocalState(dataMap)
                }
            }
        }

    private val _isFasting = MutableStateFlow(false)
    val isFasting = _isFasting.asStateFlow()

    private val _startTimeInMillis = MutableStateFlow(0L)
    val startTimeInMillis = _startTimeInMillis.asStateFlow()

    init {
        dataClient.addListener(dataListener)
        fetchData()
    }

    private fun fetchData() {
        // Fetch data in IO dispatcher
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataItems = dataClient.dataItems.await()
                dataItems.forEach { dataItem ->
                    if (dataItem.uri.path == FASTING_PATH) {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        updateLocalState(dataMap)
                        return@forEach // Exit after first relevant data item is found
                    }
                }
                dataItems.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching current data", e) // Logging error
            }
        }
    }

    private fun updateLocalState(dataMap: DataMap) {
        val isFasting = dataMap.getBoolean(IS_FASTING_KEY, false)
        val startTime = dataMap.getLong(START_TIME_KEY, 0L)

        _isFasting.value = isFasting
        _startTimeInMillis.value = startTime
        Log.d(
            TAG,
            "State updated from Data Layer: isFasting=$isFasting, startTime=$startTime"
        ) // Logging
    }

    private suspend fun updateFastingState(isFasting: Boolean, startTimeInMillis: Long) {
        val request: PutDataRequest = PutDataMapRequest.create(FASTING_PATH).apply {
            dataMap.putBoolean(IS_FASTING_KEY, isFasting)
            dataMap.putLong(START_TIME_KEY, startTimeInMillis)
        }.asPutDataRequest().setUrgent()

        try {
            val result: DataItem = dataClient.putDataItem(request).await()
            val dataMap = DataMapItem.fromDataItem(result).dataMap
            updateLocalState(dataMap)
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