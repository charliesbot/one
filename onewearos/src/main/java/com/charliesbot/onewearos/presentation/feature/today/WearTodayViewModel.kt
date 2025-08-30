package com.charliesbot.onewearos.presentation.feature.today

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.domain.usecase.FastingUseCase
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.convertMillisToLocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class WearTodayViewModel(
    private val fastingUseCase: FastingUseCase,
) : ViewModel() {

    private val currentFasting: StateFlow<FastingDataItem?> = fastingUseCase.getCurrentFastingFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )
    val isFasting: StateFlow<Boolean> = currentFasting
        .map { it?.isFasting ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)
    val startTimeInMillis: StateFlow<Long> = currentFasting
        .map { it?.startTimeInMillis ?: -1L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), -1L)
    val fastingGoalId: StateFlow<String> = currentFasting
        .map { it?.fastingGoalId ?: PredefinedFastingGoals.SIXTEEN_EIGHT.id }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            PredefinedFastingGoals.SIXTEEN_EIGHT.id
        )

    private val _temporalStartTime = MutableStateFlow<LocalDateTime?>(null)
    val temporalStartTime: StateFlow<LocalDateTime?> = _temporalStartTime.asStateFlow()

    fun initializeTemporalTime() {
        viewModelScope.launch {
            val initialMillis = startTimeInMillis.first()
            _temporalStartTime.value = convertMillisToLocalDateTime(initialMillis)
            Log.e("TAG", "initializeTemporalTime: $temporalStartTime")
        }
    }


    fun onStartFasting() {
        viewModelScope.launch {
            fastingUseCase.startFasting(fastingGoalId.value)
        }
    }

    fun onStopFasting() {
        viewModelScope.launch {
            fastingUseCase.stopFasting()
        }
    }

    fun updateTemporalDate(newDate: LocalDate) {
        val currentTime = _temporalStartTime.value?.toLocalTime() ?: LocalTime.now()
        _temporalStartTime.value = LocalDateTime.of(newDate, currentTime)
    }

    fun updateTemporalTime(newTime: LocalTime) {
        val currentDate = _temporalStartTime.value?.toLocalDate() ?: LocalDate.now()
        _temporalStartTime.value = LocalDateTime.of(currentDate, newTime)
    }

    suspend fun updateStartTime(timeInMillis: Long) {
        fastingUseCase.updateFastingConfig(startTimeMillis = timeInMillis)
    }

    suspend fun updateFastingGoal(fastingGoalId: String) {
        fastingUseCase.updateFastingConfig(goalId = fastingGoalId)
    }
}
