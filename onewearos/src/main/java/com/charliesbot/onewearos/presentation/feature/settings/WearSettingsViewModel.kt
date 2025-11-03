package com.charliesbot.onewearos.presentation.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.core.data.repositories.preferencesRepository.PreferencesRepository
import com.charliesbot.shared.core.domain.usecase.CalculateSmartNotificationTimeUseCase
import com.charliesbot.shared.core.domain.usecase.ScheduleSmartNotificationsUseCase
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

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.getSmartNotificationsEnabled(),
        preferencesRepository.getVibrationEnabled(),
        preferencesRepository.getBedtime()
    ) { smartEnabled, vibrationEnabled, bedtime ->
        val notificationTime = try {
            val nextTime = calculateSmartNotificationTimeUseCase()
            val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            nextTime.toLocalTime().format(formatter)
        } catch (e: Exception) {
            "Not scheduled"
        }
        
        SettingsUiState(
            smartNotificationsEnabled = smartEnabled,
            vibrationEnabled = vibrationEnabled,
            bedtime = bedtime,
            calculatedNotificationTime = notificationTime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun toggleSmartNotifications() {
        viewModelScope.launch {
            val currentValue = preferencesRepository.getSmartNotificationsEnabled().stateIn(viewModelScope).value
            val newValue = !currentValue
            preferencesRepository.setSmartNotificationsEnabled(newValue)
            
            // Reschedule notifications if enabled
            if (newValue) {
                scheduleSmartNotificationsUseCase()
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
            // Reschedule notifications with new bedtime
            if (preferencesRepository.getSmartNotificationsEnabled().stateIn(viewModelScope).value) {
                scheduleSmartNotificationsUseCase()
            }
        }
    }
}

