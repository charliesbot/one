package com.charliesbot.one.today

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TodayViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {
    private val _startTimeInMillis = MutableStateFlow(sharedPreferences.getLong("start_time", 0L))
    val startTimeInMillis: StateFlow<Long> = _startTimeInMillis

    private val _isFasting = MutableStateFlow(sharedPreferences.getBoolean("is_fasting", false))
    val isFasting: StateFlow<Boolean> = _isFasting

    fun onStopFasting() {
        _startTimeInMillis.value = 0L
        _isFasting.value = false
        updateSharedPreferences()
    }

    fun onStartFasting() {
        _startTimeInMillis.value = System.currentTimeMillis()
        _isFasting.value = true
        updateSharedPreferences()
    }

    private fun updateSharedPreferences() {
        sharedPreferences.edit {
            putLong("start_time", _startTimeInMillis.value)
            putBoolean("is_fasting", _isFasting.value)
            apply()
        }
    }
}