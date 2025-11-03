package com.charliesbot.shared.core.data.repositories.preferencesRepository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.charliesbot.shared.core.constants.DataStoreConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime

class PreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    private object PrefKeys {
        val SMART_NOTIFICATIONS_ENABLED = booleanPreferencesKey(DataStoreConstants.SMART_NOTIFICATIONS_ENABLED_KEY)
        val VIBRATION_ENABLED = booleanPreferencesKey(DataStoreConstants.NOTIFICATION_VIBRATION_ENABLED_KEY)
        val BEDTIME_HOUR = intPreferencesKey(DataStoreConstants.BEDTIME_HOUR_KEY)
        val BEDTIME_MINUTE = intPreferencesKey(DataStoreConstants.BEDTIME_MINUTE_KEY)
    }

    override fun getSmartNotificationsEnabled(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[PrefKeys.SMART_NOTIFICATIONS_ENABLED] ?: true // Default enabled
        }
    }

    override suspend fun setSmartNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PrefKeys.SMART_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    override fun getVibrationEnabled(): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[PrefKeys.VIBRATION_ENABLED] ?: true // Default enabled
        }
    }

    override suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PrefKeys.VIBRATION_ENABLED] = enabled
        }
    }

    override fun getBedtime(): Flow<LocalTime?> {
        return dataStore.data.map { prefs ->
            val hour = prefs[PrefKeys.BEDTIME_HOUR]
            val minute = prefs[PrefKeys.BEDTIME_MINUTE]
            
            if (hour != null && minute != null) {
                LocalTime.of(hour, minute)
            } else {
                null
            }
        }
    }

    override suspend fun setBedtime(time: LocalTime?) {
        dataStore.edit { prefs ->
            if (time != null) {
                prefs[PrefKeys.BEDTIME_HOUR] = time.hour
                prefs[PrefKeys.BEDTIME_MINUTE] = time.minute
            } else {
                // Clear bedtime
                prefs.remove(PrefKeys.BEDTIME_HOUR)
                prefs.remove(PrefKeys.BEDTIME_MINUTE)
            }
        }
    }
}

