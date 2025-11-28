package com.charliesbot.one.features.settings

import android.app.Application
import android.content.ContentValues
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepository
import com.charliesbot.shared.core.data.repositories.settingsRepository.SettingsRepository
import com.charliesbot.shared.core.domain.usecase.FastingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val notifyOnCompletion: Boolean = true,
    val notifyOneHourBefore: Boolean = true,
    val isSyncing: Boolean = false,
    val isExporting: Boolean = false,
    val showExportSuccess: Boolean = false,
    val showExportError: Boolean = false,
    val showSyncSuccess: Boolean = false,
    val showSyncError: Boolean = false
)

class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val fastingHistoryRepository: FastingHistoryRepository,
    private val fastingUseCase: FastingUseCase
) : AndroidViewModel(application) {

    private val _isSyncing = MutableStateFlow(false)
    private val _isExporting = MutableStateFlow(false)
    private val _showExportSuccess = MutableStateFlow(false)
    private val _showExportError = MutableStateFlow(false)
    private val _showSyncSuccess = MutableStateFlow(false)
    private val _showSyncError = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.notificationsEnabled,
        settingsRepository.notifyOnCompletion,
        settingsRepository.notifyOneHourBefore,
        _isSyncing,
        _isExporting,
        _showExportSuccess,
        _showExportError,
        _showSyncSuccess,
        _showSyncError
    ) { flows ->
        SettingsUiState(
            notificationsEnabled = flows[0] as Boolean,
            notifyOnCompletion = flows[1] as Boolean,
            notifyOneHourBefore = flows[2] as Boolean,
            isSyncing = flows[3] as Boolean,
            isExporting = flows[4] as Boolean,
            showExportSuccess = flows[5] as Boolean,
            showExportError = flows[6] as Boolean,
            showSyncSuccess = flows[7] as Boolean,
            showSyncError = flows[8] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

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

    fun exportHistory() {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val records = fastingHistoryRepository.getAllHistory().first()
                if (records.isEmpty()) {
                    Log.d(LOG_TAG, "SettingsViewModel: No records to export")
                    _showExportError.value = true
                    _isExporting.value = false
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
                    _showExportError.value = true
                    _isExporting.value = false
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

                _showExportSuccess.value = true
                Log.d(LOG_TAG, "SettingsViewModel: Export successful - saved to Downloads/$fileName")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "SettingsViewModel: Export failed", e)
                _showExportError.value = true
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
                _showSyncSuccess.value = true
                Log.d(LOG_TAG, "SettingsViewModel: Force sync successful")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "SettingsViewModel: Force sync failed", e)
                _showSyncError.value = true
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun dismissExportSuccess() {
        _showExportSuccess.value = false
    }

    fun dismissExportError() {
        _showExportError.value = false
    }

    fun dismissSyncSuccess() {
        _showSyncSuccess.value = false
    }

    fun dismissSyncError() {
        _showSyncError.value = false
    }
}

