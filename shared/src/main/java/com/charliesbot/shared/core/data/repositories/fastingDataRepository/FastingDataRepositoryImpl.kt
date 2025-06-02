package com.charliesbot.shared.core.data.repositories.fastingDataRepository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.DataStoreConstants
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.models.FastingDataItem
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FastingDataRepositoryImpl(
    context: Context,
    private val dataStore: DataStore<Preferences>
) : FastingDataRepository {

    private val dataClient: DataClient = Wearable.getDataClient(context.applicationContext)

    override val isFasting: Flow<Boolean> = dataStore.data
        .catch { exception -> handleDataStoreError(exception, "isFasting") }
        .map {
            it[PrefKeys.IS_FASTING] == true
        }
    override val startTimeInMillis: Flow<Long> = dataStore.data
        .catch { exception -> handleDataStoreError(exception, "startTimeInMillis") }
        .map {
            it[PrefKeys.START_TIME] ?: -1
        }
    override val fastingGoalId: Flow<String> = dataStore.data
        .catch { exception -> handleDataStoreError(exception, "fastingGoalId") }
        .map {
            it[PrefKeys.FASTING_GOAL_ID] ?: PredefinedFastingGoals.SIXTEEN_EIGHT.id
        }
    override val lastUpdateTimestamp: Flow<Long> = dataStore.data
        .catch { exception -> handleDataStoreError(exception, "lastUpdateTimestamp") }
        .map { it[PrefKeys.LAST_UPDATED_TIMESTAMP] ?: -1 }

    override suspend fun getCurrentFasting(): FastingDataItem {
        return try {
            val prefs = dataStore.data.first()
            val isFasting = prefs[PrefKeys.IS_FASTING] == true
            val startTime = prefs[PrefKeys.START_TIME] ?: -1
            val timestamp = prefs[PrefKeys.LAST_UPDATED_TIMESTAMP] ?: -1
            val fastingGoalId =
                prefs[PrefKeys.FASTING_GOAL_ID] ?: PredefinedFastingGoals.SIXTEEN_EIGHT.id
            FastingDataItem(isFasting, startTime, timestamp, fastingGoalId)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Repo: Error reading current state snapshot", e)
            FastingDataItem() // Return default on error
        }
    }

    override suspend fun startFasting(startTimeInMillis: Long, fastingGoalId: String) {
        updateLocalAndRemoteStore(
            isFasting = true,
            startTimeInMillis = startTimeInMillis,
            fastingGoalId = fastingGoalId
        )
    }

    override suspend fun stopFasting(fastingGoalId: String) {
        updateLocalAndRemoteStore(
            isFasting = false,
            startTimeInMillis = -1, // -1 indicates not set
            fastingGoalId = fastingGoalId
        )
    }

    override suspend fun updateFastingSchedule(startTimeInMillis: Long) {
        val currentGoalId = this.fastingGoalId.first()
        updateLocalAndRemoteStore(
            isFasting = true,
            startTimeInMillis = startTimeInMillis,
            fastingGoalId = currentGoalId
        )
    }

    override suspend fun updateFastingGoalId(fastingGoalId: String) {
        val currentData = getCurrentFasting()
        updateLocalAndRemoteStore(
            isFasting = currentData.isFasting,
            startTimeInMillis = currentData.startTimeInMillis,
            fastingGoalId = fastingGoalId,
        )
    }

    override suspend fun updateFastingStatusFromRemote(
        startTimeInMillis: Long,
        fastingGoalId: String,
        isFasting: Boolean,
        lastUpdateTimestamp: Long
    ) {
        updateLocalStore(
            isFasting = isFasting,
            startTimeInMillis = startTimeInMillis,
            fastingGoalId = fastingGoalId,
            lastUpdateTimestamp = lastUpdateTimestamp
        )
    }

    private suspend fun updateLocalAndRemoteStore(
        isFasting: Boolean,
        startTimeInMillis: Long,
        fastingGoalId: String?,
    ) {
        val prefs = dataStore.data.first()
        val lastUpdateTimestamp = System.currentTimeMillis()
        val validFastingGoalId: String = fastingGoalId ?: prefs[PrefKeys.FASTING_GOAL_ID]!!
        updateLocalStore(isFasting, startTimeInMillis, validFastingGoalId, lastUpdateTimestamp)
        updateRemoteStore(isFasting, startTimeInMillis, validFastingGoalId, lastUpdateTimestamp)
    }

    private suspend fun updateLocalStore(
        isFasting: Boolean,
        startTimeInMillis: Long,
        fastingGoalId: String,
        lastUpdateTimestamp: Long
    ) {
        try {
            dataStore.edit { prefs ->
                prefs[PrefKeys.IS_FASTING] = isFasting
                prefs[PrefKeys.START_TIME] = startTimeInMillis
                prefs[PrefKeys.FASTING_GOAL_ID] = fastingGoalId
                prefs[PrefKeys.LAST_UPDATED_TIMESTAMP] = lastUpdateTimestamp
            }
            Log.d(LOG_TAG, "Repo: DataStore updated successfully")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Repo: Error updating DataStore: ", e)
        }
    }

    // In this context, remote data store is the Data Layer
    // which is in charge of syncing data between the watch and the phone.
    private suspend fun updateRemoteStore(
        isFasting: Boolean,
        startTimeInMillis: Long,
        fastingGoalId: String,
        lastUpdateTimestamp: Long
    ) {
        val request: PutDataRequest =
            PutDataMapRequest.create(DataStoreConstants.FASTING_PATH_KEY).apply {
                dataMap.putBoolean(DataStoreConstants.IS_FASTING_KEY, isFasting)
                dataMap.putLong(DataStoreConstants.START_TIME_KEY, startTimeInMillis)
                dataMap.putString(DataStoreConstants.FASTING_GOAL_KEY, fastingGoalId)
                dataMap.putLong(DataStoreConstants.UPDATE_TIMESTAMP_KEY, lastUpdateTimestamp)
            }.asPutDataRequest().setUrgent()

        try {
            dataClient.putDataItem(request).await()
            Log.d(
                LOG_TAG,
                "State updated in Data Layer: isFasting=$isFasting, startTime=$startTimeInMillis, fastingGoalId=$fastingGoalId, timestamp=$lastUpdateTimestamp"
            )
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error updating fasting state in Data Layer", e)
        }
    }

    private fun handleDataStoreError(exception: Throwable, flowName: String) {
        if (exception is IOException) {
            Log.e(
                LOG_TAG,
                "Repo: IOException reading DataStore for $flowName",
                exception
            )
            // Optionally emit a default value or let the flow complete
            // Depending on the context, re-throwing might be appropriate if it's unexpected
        } else {
            // Re-throw other critical exceptions
            Log.e(
                LOG_TAG,
                "Repo: Unexpected error reading DataStore for $flowName",
                exception
            )
            throw exception
        }
    }

    private object PrefKeys {
        val IS_FASTING = booleanPreferencesKey(DataStoreConstants.IS_FASTING_KEY)
        val START_TIME = longPreferencesKey(DataStoreConstants.START_TIME_KEY)
        val FASTING_GOAL_ID = stringPreferencesKey(DataStoreConstants.FASTING_GOAL_KEY)
        val LAST_UPDATED_TIMESTAMP = longPreferencesKey(DataStoreConstants.UPDATE_TIMESTAMP_KEY)
    }
}