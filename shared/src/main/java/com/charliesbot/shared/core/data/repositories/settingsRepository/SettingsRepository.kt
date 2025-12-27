package com.charliesbot.shared.core.data.repositories.settingsRepository

import kotlinx.coroutines.flow.Flow

/**
 * Mode for calculating smart reminder times.
 */
enum class SmartReminderMode {
    /** Automatically choose: use moving average if enough data, otherwise bedtime */
    AUTO,
    /** Always use bedtime minus 4 hours */
    BEDTIME_ONLY,
    /** Always use moving average (falls back to bedtime if not enough data) */
    MOVING_AVERAGE_ONLY
}

interface SettingsRepository {
    val notificationsEnabled: Flow<Boolean>
    val notifyOnCompletion: Flow<Boolean>
    val notifyOneHourBefore: Flow<Boolean>

    // Smart Reminders
    val smartRemindersEnabled: Flow<Boolean>
    /**
     * Bedtime stored as minutes from midnight.
     * For times after midnight (e.g., 12:30 AM), we store as next-day value (e.g., 30 for 00:30).
     * Default is 1320 (10:00 PM).
     */
    val bedtimeMinutes: Flow<Int>
    /**
     * Mode for calculating smart reminder time.
     */
    val smartReminderMode: Flow<SmartReminderMode>

    suspend fun setNotificationsEnabled(enabled: Boolean, syncToRemote: Boolean = true)
    suspend fun setNotifyOnCompletion(enabled: Boolean, syncToRemote: Boolean = true)
    suspend fun setNotifyOneHourBefore(enabled: Boolean, syncToRemote: Boolean = true)

    suspend fun setSmartRemindersEnabled(enabled: Boolean, syncToRemote: Boolean = true)
    suspend fun setBedtimeMinutes(minutes: Int, syncToRemote: Boolean = true)
    suspend fun setSmartReminderMode(mode: SmartReminderMode, syncToRemote: Boolean = true)
}

