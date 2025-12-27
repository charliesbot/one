package com.charliesbot.shared.core.data.repositories.settingsRepository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SettingsRepositoryImpl(
    context: Context,
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private val dataClient: DataClient = Wearable.getDataClient(context.applicationContext)

    override val notificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { exception -> handleDataStoreError(exception, "notificationsEnabled") }
        .map { it[PrefKeys.NOTIFICATIONS_ENABLED] ?: true }

    override val notifyOnCompletion: Flow<Boolean> = dataStore.data
        .catch { exception -> handleDataStoreError(exception, "notifyOnCompletion") }
        .map { it[PrefKeys.NOTIFY_COMPLETION] ?: true }

    override val notifyOneHourBefore: Flow<Boolean> = dataStore.data
        .catch { exception -> handleDataStoreError(exception, "notifyOneHourBefore") }
        .map { it[PrefKeys.NOTIFY_ONE_HOUR_BEFORE] ?: true }

    override val smartRemindersEnabled: Flow<Boolean> = dataStore.data
        .catch { exception -> handleDataStoreError(exception, "smartRemindersEnabled") }
        .map { it[PrefKeys.SMART_REMINDERS_ENABLED] ?: false }

    override val bedtimeMinutes: Flow<Int> = dataStore.data
        .catch { exception -> handleDataStoreError(exception, "bedtimeMinutes") }
        .map { it[PrefKeys.BEDTIME_MINUTES] ?: DEFAULT_BEDTIME_MINUTES }

    override val smartReminderMode: Flow<SmartReminderMode> = dataStore.data
        .catch { exception -> handleDataStoreError(exception, "smartReminderMode") }
        .map { prefs ->
            val modeString = prefs[PrefKeys.SMART_REMINDER_MODE] ?: SmartReminderMode.AUTO.name
            try {
                SmartReminderMode.valueOf(modeString)
            } catch (e: IllegalArgumentException) {
                SmartReminderMode.AUTO
            }
        }

    override suspend fun setNotificationsEnabled(enabled: Boolean, syncToRemote: Boolean) {
        updateLocalStore(PrefKeys.NOTIFICATIONS_ENABLED, enabled)
        if (syncToRemote) {
            syncSettingsToRemote()
        }
    }

    override suspend fun setNotifyOnCompletion(enabled: Boolean, syncToRemote: Boolean) {
        updateLocalStore(PrefKeys.NOTIFY_COMPLETION, enabled)
        if (syncToRemote) {
            syncSettingsToRemote()
        }
    }

    override suspend fun setNotifyOneHourBefore(enabled: Boolean, syncToRemote: Boolean) {
        updateLocalStore(PrefKeys.NOTIFY_ONE_HOUR_BEFORE, enabled)
        if (syncToRemote) {
            syncSettingsToRemote()
        }
    }

    override suspend fun setSmartRemindersEnabled(enabled: Boolean, syncToRemote: Boolean) {
        updateLocalStore(PrefKeys.SMART_REMINDERS_ENABLED, enabled)
        Log.d(LOG_TAG, "SettingsRepo: Smart reminders set to $enabled")
        if (syncToRemote) {
            syncSettingsToRemote()
        }
    }

    override suspend fun setBedtimeMinutes(minutes: Int, syncToRemote: Boolean) {
        try {
            dataStore.edit { prefs ->
                prefs[PrefKeys.BEDTIME_MINUTES] = minutes
            }
            Log.d(LOG_TAG, "SettingsRepo: Bedtime set to $minutes minutes from midnight")
            if (syncToRemote) {
                syncSettingsToRemote()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "SettingsRepo: Error updating bedtime in DataStore", e)
        }
    }

    override suspend fun setSmartReminderMode(mode: SmartReminderMode, syncToRemote: Boolean) {
        try {
            dataStore.edit { prefs ->
                prefs[PrefKeys.SMART_REMINDER_MODE] = mode.name
            }
            Log.d(LOG_TAG, "SettingsRepo: Smart reminder mode set to $mode")
            if (syncToRemote) {
                syncSettingsToRemote()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "SettingsRepo: Error updating smart reminder mode in DataStore", e)
        }
    }

    private suspend fun updateLocalStore(key: Preferences.Key<Boolean>, value: Boolean) {
        try {
            dataStore.edit { prefs ->
                prefs[key] = value
            }
            Log.d(LOG_TAG, "SettingsRepo: Updated $key to $value")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "SettingsRepo: Error updating DataStore", e)
        }
    }

    private suspend fun syncSettingsToRemote() {
        try {
            val prefs = dataStore.data.first() // Get actual snapshot, not Flow
            val notificationsEnabled = prefs[PrefKeys.NOTIFICATIONS_ENABLED] ?: true
            val notifyCompletion = prefs[PrefKeys.NOTIFY_COMPLETION] ?: true
            val notifyOneHourBefore = prefs[PrefKeys.NOTIFY_ONE_HOUR_BEFORE] ?: true
            val smartRemindersEnabled = prefs[PrefKeys.SMART_REMINDERS_ENABLED] ?: false
            val bedtimeMinutes = prefs[PrefKeys.BEDTIME_MINUTES] ?: DEFAULT_BEDTIME_MINUTES
            val smartReminderMode = prefs[PrefKeys.SMART_REMINDER_MODE] ?: SmartReminderMode.AUTO.name

            val request: PutDataRequest =
                PutDataMapRequest.create(SETTINGS_PATH_KEY).apply {
                    dataMap.putBoolean(NOTIFICATIONS_ENABLED_KEY, notificationsEnabled)
                    dataMap.putBoolean(NOTIFY_COMPLETION_KEY, notifyCompletion)
                    dataMap.putBoolean(NOTIFY_ONE_HOUR_BEFORE_KEY, notifyOneHourBefore)
                    dataMap.putBoolean(SMART_REMINDERS_ENABLED_KEY, smartRemindersEnabled)
                    dataMap.putInt(BEDTIME_MINUTES_KEY, bedtimeMinutes)
                    dataMap.putString(SMART_REMINDER_MODE_KEY, smartReminderMode)
                    dataMap.putLong(TIMESTAMP_KEY, System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(
                LOG_TAG,
                "SettingsRepo: Settings synced to Data Layer - notifications: $notificationsEnabled, completion: $notifyCompletion, oneHour: $notifyOneHourBefore, smartReminders: $smartRemindersEnabled, bedtime: $bedtimeMinutes, mode: $smartReminderMode"
            )
        } catch (e: Exception) {
            Log.e(LOG_TAG, "SettingsRepo: Error syncing settings to Data Layer", e)
        }
    }

    private fun handleDataStoreError(exception: Throwable, flowName: String) {
        if (exception is IOException) {
            Log.e(LOG_TAG, "SettingsRepo: IOException reading DataStore for $flowName", exception)
        } else {
            Log.e(LOG_TAG, "SettingsRepo: Unexpected error reading DataStore for $flowName", exception)
            throw exception
        }
    }

    private object PrefKeys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFY_COMPLETION = booleanPreferencesKey("notify_completion")
        val NOTIFY_ONE_HOUR_BEFORE = booleanPreferencesKey("notify_one_hour_before")
        val SMART_REMINDERS_ENABLED = booleanPreferencesKey("smart_reminders_enabled")
        val BEDTIME_MINUTES = intPreferencesKey("bedtime_minutes")
        val SMART_REMINDER_MODE = stringPreferencesKey("smart_reminder_mode")
    }

    companion object {
        private const val SETTINGS_PATH_KEY = "/settings"
        private const val NOTIFICATIONS_ENABLED_KEY = "notifications_enabled"
        private const val NOTIFY_COMPLETION_KEY = "notify_completion"
        private const val NOTIFY_ONE_HOUR_BEFORE_KEY = "notify_one_hour_before"
        private const val SMART_REMINDERS_ENABLED_KEY = "smart_reminders_enabled"
        private const val BEDTIME_MINUTES_KEY = "bedtime_minutes"
        private const val SMART_REMINDER_MODE_KEY = "smart_reminder_mode"
        private const val TIMESTAMP_KEY = "timestamp"

        // 10:00 PM = 22 * 60 = 1320 minutes from midnight
        const val DEFAULT_BEDTIME_MINUTES = 1320
    }
}

