package com.charliesbot.one.today

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.one.widgets.OneWidget
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.notifications.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodayViewModel(
    application: Application,
    private val notificationScheduler: NotificationScheduler,
    private val fastingDataRepository: FastingDataRepository
) : AndroidViewModel(application) {
    val isFasting: StateFlow<Boolean> = fastingDataRepository.isFasting.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = false
    )
    val startTimeInMillis: StateFlow<Long> = fastingDataRepository.startTimeInMillis.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = -1
    )
    val _isTimePickerDialogOpen = MutableStateFlow(false)
    val isTimePickerDialogOpen: StateFlow<Boolean> = _isTimePickerDialogOpen
    val endTimeMillis: Long = 60 * 60 * 16000 // 16 hours

    fun openTimePickerDialog() {
        _isTimePickerDialogOpen.value = true
    }

    fun closeTimePickerDialog() {
        _isTimePickerDialogOpen.value = false
    }

    suspend fun updateWidget() {
        OneWidget().updateAll(getApplication<Application>().applicationContext)
    }

    fun onStopFasting() {
        viewModelScope.launch {
            fastingDataRepository.stopFasting()
            updateWidget()
            notificationScheduler.cancelAllNotifications()
        }
    }

    fun onStartFasting() {
        val startTimeMillis = System.currentTimeMillis()
        viewModelScope.launch {
            fastingDataRepository.startFasting(startTimeMillis, endTimeMillis)
            updateWidget()
            notificationScheduler.scheduleNotifications(startTimeMillis, endTimeMillis)
        }
    }

    fun updateStartTime(timeInMillis: Long) {
        viewModelScope.launch {
            fastingDataRepository.updateFastingSchedule(timeInMillis, endTimeMillis)
            notificationScheduler.scheduleNotifications(timeInMillis, endTimeMillis)
        }
    }
}