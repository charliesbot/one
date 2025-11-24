package com.charliesbot.shared.core.data.repositories.settingsRepository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val notificationsEnabled: Flow<Boolean>
    val notifyOnCompletion: Flow<Boolean>
    val notifyOneHourBefore: Flow<Boolean>

    suspend fun setNotificationsEnabled(enabled: Boolean, syncToRemote: Boolean = true)
    suspend fun setNotifyOnCompletion(enabled: Boolean, syncToRemote: Boolean = true)
    suspend fun setNotifyOneHourBefore(enabled: Boolean, syncToRemote: Boolean = true)
}

