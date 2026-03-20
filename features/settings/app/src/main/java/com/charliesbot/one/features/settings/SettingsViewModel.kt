package com.charliesbot.one.features.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.core.abstraction.AppVersionProvider
import com.charliesbot.shared.core.abstraction.ClipboardHelper
import com.charliesbot.shared.core.abstraction.HistoryExporter
import com.charliesbot.shared.core.abstraction.StringProvider
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.domain.repository.FastingHistoryRepository
import com.charliesbot.shared.core.domain.repository.SettingsRepository
import com.charliesbot.shared.core.domain.repository.SmartReminderMode
import com.charliesbot.shared.core.domain.usecase.GetSuggestedFastingStartTimeUseCase
import com.charliesbot.shared.core.domain.usecase.SyncFastingStateUseCase
import com.charliesbot.shared.core.models.SuggestedFastingTime
import com.charliesbot.shared.core.services.SmartReminderCallback
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
  val notificationsEnabled: Boolean = true,
  val notifyOnCompletion: Boolean = true,
  val notifyOneHourBefore: Boolean = true,
  val smartRemindersEnabled: Boolean = false,
  val bedtimeMinutes: Int = 1320, // 10:00 PM default
  val smartReminderMode: SmartReminderMode = SmartReminderMode.AUTO,
  val fixedFastingStartMinutes: Int = 1140, // 7:00 PM default
  val isSyncing: Boolean = false,
  val isExporting: Boolean = false,
  val versionName: String = "Unknown",
)

class SettingsViewModel(
  private val settingsRepository: SettingsRepository,
  private val fastingHistoryRepository: FastingHistoryRepository,
  private val syncFastingStateUseCase: SyncFastingStateUseCase,
  private val smartReminderCallback: SmartReminderCallback,
  private val getSuggestedFastingStartTimeUseCase: GetSuggestedFastingStartTimeUseCase,
  private val stringProvider: StringProvider,
  private val appVersionProvider: AppVersionProvider,
  private val historyExporter: HistoryExporter,
  private val clipboardHelper: ClipboardHelper,
) : ViewModel() {

  private val _isSyncing = MutableStateFlow(false)
  private val _isExporting = MutableStateFlow(false)

  private val _sideEffects = Channel<SettingsSideEffect>(Channel.BUFFERED)
  val sideEffects = _sideEffects.receiveAsFlow()

  private val _suggestedFastingTime = MutableStateFlow<SuggestedFastingTime?>(null)
  val suggestedFastingTime: StateFlow<SuggestedFastingTime?> = _suggestedFastingTime

  val uiState: StateFlow<SettingsUiState> =
    combine(
        settingsRepository.notificationsEnabled,
        settingsRepository.notifyOnCompletion,
        settingsRepository.notifyOneHourBefore,
        settingsRepository.smartRemindersEnabled,
        settingsRepository.bedtimeMinutes,
      ) { notifications, onCompletion, oneHour, smartReminders, bedtime ->
        SettingsUiState(
          notificationsEnabled = notifications,
          notifyOnCompletion = onCompletion,
          notifyOneHourBefore = oneHour,
          smartRemindersEnabled = smartReminders,
          bedtimeMinutes = bedtime,
          versionName = appVersionProvider.versionName,
        )
      }
      .combine(settingsRepository.smartReminderMode) { state, mode ->
        state.copy(smartReminderMode = mode)
      }
      .combine(settingsRepository.fixedFastingStartMinutes) { state, fixedMinutes ->
        state.copy(fixedFastingStartMinutes = fixedMinutes)
      }
      .combine(_isSyncing) { state, isSyncing -> state.copy(isSyncing = isSyncing) }
      .combine(_isExporting) { state, isExporting -> state.copy(isExporting = isExporting) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(),
      )

  init {
    // Load suggestion initially
    refreshSuggestion()

    // Reload when settings change
    viewModelScope.launch { settingsRepository.bedtimeMinutes.collect { refreshSuggestion() } }
    viewModelScope.launch {
      settingsRepository.fixedFastingStartMinutes.collect { refreshSuggestion() }
    }
    viewModelScope.launch { settingsRepository.smartReminderMode.collect { refreshSuggestion() } }
  }

  fun setNotificationsEnabled(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setNotificationsEnabled(enabled) }
  }

  fun setNotifyOnCompletion(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setNotifyOnCompletion(enabled) }
  }

  fun setNotifyOneHourBefore(enabled: Boolean) {
    viewModelScope.launch { settingsRepository.setNotifyOneHourBefore(enabled) }
  }

  fun setSmartRemindersEnabled(enabled: Boolean) {
    viewModelScope.launch {
      settingsRepository.setSmartRemindersEnabled(enabled)
      Log.d(LOG_TAG, "SettingsViewModel: Smart reminders set to $enabled, triggering recalculation")
      smartReminderCallback.onSmartReminderSettingsChanged()
      refreshSuggestion()
    }
  }

  fun setBedtimeMinutes(minutes: Int) {
    viewModelScope.launch {
      settingsRepository.setBedtimeMinutes(minutes)
      Log.d(LOG_TAG, "SettingsViewModel: Bedtime set to $minutes minutes, triggering recalculation")
      smartReminderCallback.onSmartReminderSettingsChanged()
      refreshSuggestion()
    }
  }

  fun setSmartReminderMode(mode: SmartReminderMode) {
    viewModelScope.launch {
      settingsRepository.setSmartReminderMode(mode)
      Log.d(
        LOG_TAG,
        "SettingsViewModel: Smart reminder mode set to $mode, triggering recalculation",
      )
      smartReminderCallback.onSmartReminderSettingsChanged()
      refreshSuggestion()
    }
  }

  fun setFixedFastingStartMinutes(minutes: Int) {
    viewModelScope.launch {
      settingsRepository.setFixedFastingStartMinutes(minutes)
      Log.d(
        LOG_TAG,
        "SettingsViewModel: Fixed fasting start set to $minutes minutes, triggering recalculation",
      )
      smartReminderCallback.onSmartReminderSettingsChanged()
      refreshSuggestion()
    }
  }

  fun exportHistory() {
    viewModelScope.launch {
      _isExporting.value = true
      try {
        val records = fastingHistoryRepository.getAllHistory().first()
        if (records.isEmpty()) {
          Log.d(LOG_TAG, "SettingsViewModel: No records to export")
          _sideEffects.send(
            SettingsSideEffect.ShowSnackbar(stringProvider.getString(SettingsStrings.EXPORT_ERROR))
          )
          return@launch
        }

        historyExporter
          .export(records)
          .onSuccess {
            _sideEffects.send(
              SettingsSideEffect.ShowSnackbar(
                stringProvider.getString(SettingsStrings.EXPORT_SUCCESS)
              )
            )
          }
          .onFailure {
            _sideEffects.send(
              SettingsSideEffect.ShowSnackbar(
                stringProvider.getString(SettingsStrings.EXPORT_ERROR)
              )
            )
          }
      } catch (e: Exception) {
        Log.e(LOG_TAG, "SettingsViewModel: Export failed", e)
        _sideEffects.send(
          SettingsSideEffect.ShowSnackbar(stringProvider.getString(SettingsStrings.EXPORT_ERROR))
        )
      } finally {
        _isExporting.value = false
      }
    }
  }

  fun forceSyncToWatch() {
    viewModelScope.launch {
      _isSyncing.value = true
      syncFastingStateUseCase()
        .onSuccess {
          _sideEffects.send(
            SettingsSideEffect.ShowSnackbar(stringProvider.getString(SettingsStrings.SYNC_SUCCESS))
          )
          Log.d(LOG_TAG, "SettingsViewModel: Force sync successful")
        }
        .onFailure { e ->
          Log.e(LOG_TAG, "SettingsViewModel: Force sync failed", e)
          _sideEffects.send(
            SettingsSideEffect.ShowSnackbar(stringProvider.getString(SettingsStrings.SYNC_ERROR))
          )
        }
      _isSyncing.value = false
    }
  }

  fun copyVersionToClipboard() {
    viewModelScope.launch {
      try {
        clipboardHelper.copy("App Version", appVersionProvider.versionName)
        _sideEffects.send(
          SettingsSideEffect.ShowSnackbar(stringProvider.getString(SettingsStrings.VERSION_COPIED))
        )
        Log.d(LOG_TAG, "SettingsViewModel: Version copied to clipboard")
      } catch (e: Exception) {
        Log.e(LOG_TAG, "SettingsViewModel: Failed to copy version to clipboard", e)
      }
    }
  }

  private fun refreshSuggestion() {
    viewModelScope.launch {
      try {
        _suggestedFastingTime.value = getSuggestedFastingStartTimeUseCase.execute()
      } catch (e: Exception) {
        Log.e(LOG_TAG, "SettingsViewModel: Failed to load suggestion", e)
      }
    }
  }
}
