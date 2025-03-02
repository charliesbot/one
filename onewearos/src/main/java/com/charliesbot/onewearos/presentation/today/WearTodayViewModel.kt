package com.charliesbot.onewearos.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.core.datalayer.FastingDataClient
import kotlinx.coroutines.launch

class WearTodayViewModel(
    private val fastingDataClient: FastingDataClient
) : ViewModel() {
    val isFasting = fastingDataClient.isFasting
    val startTimeInMillis = fastingDataClient.startTimeInMillis

    override fun onCleared() {
        super.onCleared()
        fastingDataClient.cleanup()
    }

    fun onStartFasting() {
        viewModelScope.launch {
            fastingDataClient.startFasting()
        }
    }

    fun onStopFasting() {
        viewModelScope.launch {
            fastingDataClient.stopFasting()
        }
    }
}
