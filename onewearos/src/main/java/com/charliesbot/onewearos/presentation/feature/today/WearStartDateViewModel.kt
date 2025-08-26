package com.charliesbot.onewearos.presentation.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.core.domain.usecase.FastingUseCase
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class WearStartDateViewModel(
    private val fastingUseCase: FastingUseCase,
) : ViewModel() {
    private val currentFasting: StateFlow<FastingDataItem?> = fastingUseCase.getCurrentFastingFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )
    val startTimeInMillis: StateFlow<Long> = currentFasting
        .map { it?.startTimeInMillis ?: -1L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), -1L)

    fun updateStartDate(date: LocalDate) {
        viewModelScope.launch {
            val current = currentFasting.first()
            if (current != null) {
                val currentDateTime = convertMillisToLocalDateTime(current.startTimeInMillis)
                val newDateTime =
                    currentDateTime.toLocalDate().atTime(currentDateTime.toLocalTime()).with(date)
                val newStartTimeMillis =
                    newDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                fastingUseCase.updateFastingConfig(startTimeMillis = newStartTimeMillis)
            }
        }
    }

    fun updateStartTime(time: LocalTime) {
        viewModelScope.launch {
            val current = currentFasting.first()
            if (current != null) {
                val currentDateTime = convertMillisToLocalDateTime(current.startTimeInMillis)
                val newDateTime =
                    currentDateTime.toLocalTime().atDate(currentDateTime.toLocalDate()).with(time)
                val newStartTimeMillis =
                    newDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                fastingUseCase.updateFastingConfig(startTimeMillis = newStartTimeMillis)
            }
        }
    }

}