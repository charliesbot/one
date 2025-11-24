package com.charliesbot.shared.core.data.repositories.settingsRepository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val notificationsEnabled: Flow<Boolean>
    val notifyOnCompletion: Flow<Boolean>
    val notifyOneHourBefore: Flow<Boolean>

    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setNotifyOnCompletion(enabled: Boolean)
    suspend fun setNotifyOneHourBefore(enabled: Boolean)
}

