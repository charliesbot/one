package com.charliesbot.onewearos.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.notifications.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WearTodayViewModel(
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

    val fastingGoalId: StateFlow<String> = fastingDataRepository.fastingGoalId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = PredefinedFastingGoals.SIXTEEN_EIGHT.id,
    )

    fun onStartFasting() {
        val startTimeMillis = System.currentTimeMillis()
        viewModelScope.launch {
            fastingDataRepository.startFasting(startTimeMillis, fastingGoalId.value)
            notificationScheduler.scheduleNotifications(
                startTimeInMillis.value,
                fastingGoalId.value
            )
        }
    }

    fun onStopFasting() {
        viewModelScope.launch {
            fastingDataRepository.stopFasting(fastingGoalId.value)
            notificationScheduler.cancelAllNotifications()
        }
    }
}
