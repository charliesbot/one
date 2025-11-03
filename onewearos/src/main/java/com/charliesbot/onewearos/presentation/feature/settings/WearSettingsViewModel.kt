package com.charliesbot.onewearos.presentation.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.core.data.repositories.preferencesRepository.PreferencesRepository
import com.charliesbot.shared.core.domain.usecase.CalculateSmartNotificationTimeUseCase
import com.charliesbot.shared.core.domain.usecase.ScheduleSmartNotificationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class SettingsUiState(
    val smartNotificationsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val bedtime: LocalTime? = null,
    val calculatedNotificationTime: String = "Calculating..."
)

class WearSettingsViewModel(
    application: Application,
    private val preferencesRepository: PreferencesRepository,
    private val calculateSmartNotificationTimeUseCase: CalculateSmartNotificationTimeUseCase,
    private val scheduleSmartNotificationsUseCase: ScheduleSmartNotificationsUseCase
) : AndroidViewModel(application) {

    private val _calculatedTime = MutableStateFlow("Calculating...")

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.getSmartNotificationsEnabled(),
        preferencesRepository.getVibrationEnabled(),
        preferencesRepository.getBedtime(),
        _calculatedTime
    ) { smartEnabled, vibrationEnabled, bedtime, calculatedTime ->
        SettingsUiState(
            smartNotificationsEnabled = smartEnabled,
            vibrationEnabled = vibrationEnabled,
            bedtime = bedtime,
            calculatedNotificationTime = calculatedTime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )
    
    init {
        // Calculate notification time asynchronously
        calculateNextNotificationTime()
    }
    
    private fun calculateNextNotificationTime() {
        viewModelScope.launch {
            try {
                val nextTime = calculateSmartNotificationTimeUseCase()
                val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                _calculatedTime.value = nextTime.toLocalTime().format(formatter)
            } catch (e: Exception) {
                _calculatedTime.value = "Not scheduled"
            }
        }
    }

    fun toggleSmartNotifications() {
        viewModelScope.launch {
            val currentValue = preferencesRepository.getSmartNotificationsEnabled().stateIn(viewModelScope).value
            val newValue = !currentValue
            preferencesRepository.setSmartNotificationsEnabled(newValue)
            
            // Reschedule notifications and recalculate time if enabled
            if (newValue) {
                scheduleSmartNotificationsUseCase()
                calculateNextNotificationTime()
            }
        }
    }

    fun toggleVibration() {
        viewModelScope.launch {
            val currentValue = preferencesRepository.getVibrationEnabled().stateIn(viewModelScope).value
            preferencesRepository.setVibrationEnabled(!currentValue)
        }
    }

    fun setBedtime(time: LocalTime?) {
        viewModelScope.launch {
            preferencesRepository.setBedtime(time)
            // Recalculate and reschedule notifications with new bedtime
            calculateNextNotificationTime()
            if (preferencesRepository.getSmartNotificationsEnabled().stateIn(viewModelScope).value) {
                scheduleSmartNotificationsUseCase()
            }
        }
    }
}

