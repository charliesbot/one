package com.charliesbot.onewearos.presentation.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.domain.usecase.FastingUseCase
import com.charliesbot.shared.core.models.FastingDataItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
}
