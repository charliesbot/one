package com.charliesbot.one.today

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import com.charliesbot.one.notifications.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TodayViewModel(
    private val sharedPreferences: SharedPreferences,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {
    private val _startTimeInMillis = MutableStateFlow(sharedPreferences.getLong("start_time", 0L))
    val startTimeInMillis: StateFlow<Long> = _startTimeInMillis

    private val _isFasting = MutableStateFlow(sharedPreferences.getBoolean("is_fasting", false))
    val isFasting: StateFlow<Boolean> = _isFasting

    private val _isTimePickerDialogOpen = MutableStateFlow(false)
    val isTimePickerDialogOpen: StateFlow<Boolean> = _isTimePickerDialogOpen

    fun openTimePickerDialog() {
        _isTimePickerDialogOpen.value = true
    }

    fun closeTimePickerDialog() {
        _isTimePickerDialogOpen.value = false
    }

    fun onStopFasting() {
        _startTimeInMillis.value = 0L
        _isFasting.value = false
        updateSharedPreferences()
        notificationScheduler.cancelAllNotifications()
    }

    fun onStartFasting() {
        _startTimeInMillis.value = System.currentTimeMillis()
        _isFasting.value = true
        updateSharedPreferences()
        notificationScheduler.scheduleNotifications(_startTimeInMillis.value)
    }

    fun updateStartTime(timeInMillis: Long) {
        _startTimeInMillis.value = timeInMillis
        updateSharedPreferences()
        notificationScheduler.scheduleNotifications(_startTimeInMillis.value)
    }

    private fun updateSharedPreferences() {
        sharedPreferences.edit {
            putLong("start_time", _startTimeInMillis.value)
            putBoolean("is_fasting", _isFasting.value)
            apply()
        }
    }
}