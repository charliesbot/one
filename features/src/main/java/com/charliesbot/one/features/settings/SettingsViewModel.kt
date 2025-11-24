package com.charliesbot.one.features.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
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
import java.io.File
import java.io.FileWriter

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

    fun exportHistory(): Intent? {
        var result: Intent? = null
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

                val csvFile = File(getApplication<Application>().cacheDir, "fasting_history.csv")
                FileWriter(csvFile).use { writer ->
                    // Write header
                    writer.append("Start Time,End Time,Duration (hours),Goal\n")
                    // Write records
                    records.forEach { record ->
                        val durationHours = (record.endTimeEpochMillis - record.startTimeEpochMillis) / (1000 * 60 * 60)
                        writer.append("${record.startTimeEpochMillis},${record.endTimeEpochMillis},$durationHours,${record.fastingGoalId}\n")
                    }
                }

                val uri = FileProvider.getUriForFile(
                    getApplication(),
                    "${getApplication<Application>().packageName}.provider",
                    csvFile
                )

                result = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                _showExportSuccess.value = true
                Log.d(LOG_TAG, "SettingsViewModel: Export successful")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "SettingsViewModel: Export failed", e)
                _showExportError.value = true
            } finally {
                _isExporting.value = false
            }
        }
        return result
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

