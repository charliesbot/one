package com.charliesbot.shared.core.services

/**
 * Callback interface for triggering smart reminder recalculation.
 * This allows the features module to request a recalculation without
 * directly depending on the app module.
 */
interface SmartReminderCallback {
    /**
     * Called when smart reminder settings change (enabled/disabled or bedtime changed).
     * The implementation should recalculate and sync the smart reminder.
     */
    fun onSmartReminderSettingsChanged()
}

