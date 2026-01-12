package com.charliesbot.one.features.settings

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.R
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepository
import com.charliesbot.shared.core.data.repositories.settingsRepository.SettingsRepository
import com.charliesbot.shared.core.data.repositories.settingsRepository.SmartReminderMode
import com.charliesbot.shared.core.domain.usecase.FastingUseCase
import com.charliesbot.shared.core.domain.usecase.GetSuggestedFastingStartTimeUseCase
import com.charliesbot.shared.core.services.SmartReminderCallback
import com.charliesbot.shared.core.models.SuggestedFastingTime
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

sealed interface SettingsSideEffect {
    data class ShowSnackbar(val messageRes: Int) : SettingsSideEffect
}

class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val fastingHistoryRepository: FastingHistoryRepository,
    private val fastingUseCase: FastingUseCase,
    private val smartReminderCallback: SmartReminderCallback,
    private val getSuggestedFastingStartTimeUseCase: GetSuggestedFastingStartTimeUseCase,
) : AndroidViewModel(application) {

    private val _isSyncing = MutableStateFlow(false)
    private val _isExporting = MutableStateFlow(false)

    private val _sideEffects = Channel<SettingsSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    private val _suggestedFastingTime = MutableStateFlow<SuggestedFastingTime?>(null)
    val suggestedFastingTime: StateFlow<SuggestedFastingTime?> = _suggestedFastingTime

    // Get version from package manager
    private val versionName: String = try {
        val context = getApplication<Application>()
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
    } catch (e: Exception) {
        Log.e(LOG_TAG, "SettingsViewModel: Failed to get version name", e)
        "Unknown"
    }

    val uiState: StateFlow<SettingsUiState> = combine(
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
            versionName = versionName
        )
    }.combine(settingsRepository.smartReminderMode) { state, mode ->
        state.copy(smartReminderMode = mode)
    }.combine(settingsRepository.fixedFastingStartMinutes) { state, fixedMinutes ->
        state.copy(fixedFastingStartMinutes = fixedMinutes)
    }.combine(_isSyncing) { state, isSyncing ->
        state.copy(isSyncing = isSyncing)
    }.combine(_isExporting) { state, isExporting ->
        state.copy(isExporting = isExporting)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        // Load suggestion initially
        refreshSuggestion()

        // Reload when settings change
        viewModelScope.launch {
            settingsRepository.bedtimeMinutes.collect {
                refreshSuggestion()
            }
        }
        viewModelScope.launch {
            settingsRepository.fixedFastingStartMinutes.collect {
                refreshSuggestion()
            }
        }
        viewModelScope.launch {
            settingsRepository.smartReminderMode.collect {
                refreshSuggestion()
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setNotifyOnCompletion(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotifyOnCompletion(enabled)
        }
    }

    fun setNotifyOneHourBefore(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotifyOneHourBefore(enabled)
        }
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
            Log.d(LOG_TAG, "SettingsViewModel: Smart reminder mode set to $mode, triggering recalculation")
            smartReminderCallback.onSmartReminderSettingsChanged()
            refreshSuggestion()
        }
    }

    fun setFixedFastingStartMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setFixedFastingStartMinutes(minutes)
            Log.d(LOG_TAG, "SettingsViewModel: Fixed fasting start set to $minutes minutes, triggering recalculation")
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
                    _sideEffects.send(SettingsSideEffect.ShowSnackbar(R.string.settings_export_error))
                    return@launch
                }

                // Create filename with timestamp
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(Date())
                val fileName = "fasting_history_$timestamp.csv"

                // Use MediaStore to save to Downloads folder (Android 10+)
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = getApplication<Application>().contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri == null) {
                    Log.e(LOG_TAG, "SettingsViewModel: Failed to create file in Downloads")
                    _sideEffects.send(SettingsSideEffect.ShowSnackbar(R.string.settings_export_error))
                    return@launch
                }

                // Write CSV data with human-readable dates
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.bufferedWriter().use { writer ->
                        // Write header
                        writer.append("Start Time,End Time,Duration (hours),Goal\n")
                        
                        // Date formatter for human-readable timestamps
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        
                        // Write records
                        records.forEach { record ->
                            val startDate = dateFormat.format(Date(record.startTimeEpochMillis))
                            val endDate = dateFormat.format(Date(record.endTimeEpochMillis))
                            val durationHours = (record.endTimeEpochMillis - record.startTimeEpochMillis) / (1000 * 60 * 60)
                            
                            writer.append("$startDate,$endDate,$durationHours,${record.fastingGoalId}\n")
                        }
                    }
                }

                // Mark file as complete (no longer pending)
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                _sideEffects.send(SettingsSideEffect.ShowSnackbar(R.string.settings_export_success))
                Log.d(LOG_TAG, "SettingsViewModel: Export successful - saved to Downloads/$fileName")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "SettingsViewModel: Export failed", e)
                _sideEffects.send(SettingsSideEffect.ShowSnackbar(R.string.settings_export_error))
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun forceSyncToWatch() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                fastingUseCase.syncCurrentState()
                _sideEffects.send(SettingsSideEffect.ShowSnackbar(R.string.settings_sync_success))
                Log.d(LOG_TAG, "SettingsViewModel: Force sync successful")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "SettingsViewModel: Force sync failed", e)
                _sideEffects.send(SettingsSideEffect.ShowSnackbar(R.string.settings_sync_error))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun copyVersionToClipboard() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("App Version", versionName)
                clipboard.setPrimaryClip(clipData)
                _sideEffects.send(SettingsSideEffect.ShowSnackbar(R.string.settings_version_copied))
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

