package com.charliesbot.one.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.one.notifications.NotificationScheduler
import com.charliesbot.shared.core.datalayer.FastingDataClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TodayViewModel(
    private val fastingDataClient: FastingDataClient,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {
    val startTimeInMillis: StateFlow<Long> = fastingDataClient.startTimeInMillis
    val isFasting: StateFlow<Boolean> = fastingDataClient.isFasting
    private val _isTimePickerDialogOpen = MutableStateFlow(false)
    val isTimePickerDialogOpen: StateFlow<Boolean> = _isTimePickerDialogOpen

    override fun onCleared() {
        super.onCleared()
        fastingDataClient.cleanup()
    }

    fun openTimePickerDialog() {
        _isTimePickerDialogOpen.value = true
    }

    fun closeTimePickerDialog() {
        _isTimePickerDialogOpen.value = false
    }

    fun onStopFasting() {
        viewModelScope.launch {
            fastingDataClient.stopFasting()
            notificationScheduler.cancelAllNotifications()
        }
    }

    fun onStartFasting() {
        viewModelScope.launch {
            fastingDataClient.startFasting()
            notificationScheduler.scheduleNotifications(fastingDataClient.startTimeInMillis.value)
        }
    }

    fun updateStartTime(timeInMillis: Long) {
        viewModelScope.launch {
            fastingDataClient.updateStartTime(timeInMillis)
            notificationScheduler.scheduleNotifications(startTimeInMillis.value)
        }
    }
}