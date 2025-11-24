package com.charliesbot.shared.core.data.repositories.settingsRepository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        updateLocalAndRemoteStore(PrefKeys.NOTIFICATIONS_ENABLED, enabled)
    }

    override suspend fun setNotifyOnCompletion(enabled: Boolean) {
        updateLocalAndRemoteStore(PrefKeys.NOTIFY_COMPLETION, enabled)
    }

    override suspend fun setNotifyOneHourBefore(enabled: Boolean) {
        updateLocalAndRemoteStore(PrefKeys.NOTIFY_ONE_HOUR_BEFORE, enabled)
    }

    private suspend fun updateLocalAndRemoteStore(key: Preferences.Key<Boolean>, value: Boolean) {
        updateLocalStore(key, value)
        syncSettingsToRemote()
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
            val prefs = dataStore.data
            val notificationsEnabled = prefs.map { it[PrefKeys.NOTIFICATIONS_ENABLED] ?: true }
            val notifyCompletion = prefs.map { it[PrefKeys.NOTIFY_COMPLETION] ?: true }
            val notifyOneHourBefore = prefs.map { it[PrefKeys.NOTIFY_ONE_HOUR_BEFORE] ?: true }

            val request: PutDataRequest =
                PutDataMapRequest.create(SETTINGS_PATH_KEY).apply {
                    dataMap.putBoolean(NOTIFICATIONS_ENABLED_KEY, notificationsEnabled.toString().toBoolean())
                    dataMap.putBoolean(NOTIFY_COMPLETION_KEY, notifyCompletion.toString().toBoolean())
                    dataMap.putBoolean(NOTIFY_ONE_HOUR_BEFORE_KEY, notifyOneHourBefore.toString().toBoolean())
                    dataMap.putLong(TIMESTAMP_KEY, System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d(LOG_TAG, "SettingsRepo: Settings synced to Data Layer")
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
    }

    companion object {
        private const val SETTINGS_PATH_KEY = "/settings"
        private const val NOTIFICATIONS_ENABLED_KEY = "notifications_enabled"
        private const val NOTIFY_COMPLETION_KEY = "notify_completion"
        private const val NOTIFY_ONE_HOUR_BEFORE_KEY = "notify_one_hour_before"
        private const val TIMESTAMP_KEY = "timestamp"
    }
}

