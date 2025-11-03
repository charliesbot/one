package com.charliesbot.shared.core.data.repositories.preferencesRepository

import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

/**
 * Repository for managing user preferences and notification settings.
 * Stores settings in DataStore for persistence across app restarts.
 */
interface PreferencesRepository {
    
    /**
     * Get whether smart notifications are enabled.
     * Smart notifications use historical data to predict optimal fasting start times.
     */
    fun getSmartNotificationsEnabled(): Flow<Boolean>
    
    /**
     * Enable or disable smart notifications.
     */
    suspend fun setSmartNotificationsEnabled(enabled: Boolean)
    
    /**
     * Get whether notification vibration/haptic feedback is enabled.
     */
    fun getVibrationEnabled(): Flow<Boolean>
    
    /**
     * Enable or disable notification vibration.
     */
    suspend fun setVibrationEnabled(enabled: Boolean)
    
    /**
     * Get the user's configured bedtime.
     * Used as a fallback for smart notifications when no historical data exists.
     * @return LocalTime if set, null if not configured
     */
    fun getBedtime(): Flow<LocalTime?>
    
    /**
     * Set the user's bedtime for notification calculations.
     * Smart notifications will be scheduled 6 hours before bedtime when no historical data exists.
     * @param time The bedtime, or null to clear
     */
    suspend fun setBedtime(time: LocalTime?)
}

