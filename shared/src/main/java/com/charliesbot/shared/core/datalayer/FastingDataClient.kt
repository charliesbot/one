package com.charliesbot.shared.core.datalayer

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FastingDataClient(val context: Context) {
    companion object {
        private const val TAG = "FastingDataClient" // Tag for logging
        private const val FASTING_PATH = "/fasting_state"
        private const val IS_FASTING = "is_fasting"
        private const val START_TIME = "start_time"
    }

    private val dataClient: DataClient = Wearable.getDataClient(context)

    private val _isFasting = MutableStateFlow(false)
    val isFasting = _isFasting.asStateFlow()

    private val _startTimeInMillis = MutableStateFlow(0L)
    val startTimeInMillis = _startTimeInMillis.asStateFlow()

    init {
        dataClient.addListener { dataEvents ->
            dataEvents.forEach {
                updateLocalState(it.dataItem)
            }
        }

        fetchData()
    }

    private fun fetchData() {
        dataClient.dataItems.addOnSuccessListener { dataEvents ->
            dataEvents.forEach {
                updateLocalState(it)
            }
        }
    }

    @SuppressLint("VisibleForTests")
    private fun updateLocalState(dataItem: DataItem) {
        if (dataItem.uri.path == FASTING_PATH) {
            return;
        }
        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
        val isFasting = dataMap.getBoolean(IS_FASTING, false)
        val startTime = dataMap.getLong(START_TIME, 0L)
        _isFasting.value = isFasting
        _startTimeInMillis.value = startTime
        Log.d(TAG, "State updated from Data Layer: isFasting=$isFasting, startTime=$startTime")
    }

//    suspend fun startFasting(startTimeInMillis: Long = System.currentTimeMillis()) {
//        updateFastingState(true, startTimeInMillis)
//    }

    private suspend fun updateFastingState(isFasting: Boolean, startTimeInMillis: Long) {
        val request: PutDataRequest = PutDataMapRequest.create(FASTING_PATH).apply {
            dataMap.putBoolean(IS_FASTING, isFasting)
            dataMap.putLong(START_TIME, startTimeInMillis)
        }.asPutDataRequest().setUrgent()

        try {
            val result = dataClient.putDataItem(request).await()

        } catch(e: Exception) {

        }
    }
}