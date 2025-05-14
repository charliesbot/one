package com.charliesbot.one.today

import android.content.Context
import androidx.glance.appwidget.updateAll
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
    private val context: Context,
    private val notificationScheduler: NotificationScheduler,
    private val fastingDataRepository: FastingDataRepository
) : ViewModel() {
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

    fun openTimePickerDialog() {
        _isTimePickerDialogOpen.value = true
    }

    fun closeTimePickerDialog() {
        _isTimePickerDialogOpen.value = false
    }

    fun onStopFasting() {
        viewModelScope.launch {
            fastingDataRepository.stopFasting()
            OneWidget().updateAll(context)
            notificationScheduler.cancelAllNotifications()
        }
    }

    fun onStartFasting() {
        val startTimeMillis = System.currentTimeMillis()
        viewModelScope.launch {
            fastingDataRepository.startFasting(startTimeMillis)
            OneWidget().updateAll(context)
            notificationScheduler.scheduleNotifications(startTimeMillis)
        }
    }

    fun updateStartTime(timeInMillis: Long) {
        viewModelScope.launch {
            fastingDataRepository.updateStartTime(timeInMillis)
            notificationScheduler.scheduleNotifications(timeInMillis)
        }
    }
}