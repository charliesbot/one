package com.charliesbot.shared.core.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.DataLayerConstants
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.domain.model.CustomGoalData
import com.charliesbot.shared.core.domain.model.toData
import com.charliesbot.shared.core.domain.model.toFastGoal
import com.charliesbot.shared.core.domain.repository.CustomGoalRepository
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CustomGoalRepositoryImpl(context: Context, private val dataStore: DataStore<Preferences>) :
  CustomGoalRepository {

  private val dataClient = Wearable.getDataClient(context.applicationContext)
  private val json = Json { ignoreUnknownKeys = true }

  override val customGoals: Flow<List<FastGoal>> =
    dataStore.data
      .catch { e ->
        Log.e(LOG_TAG, "CustomGoalRepo: Error reading custom goals", e)
        emit(androidx.datastore.preferences.core.emptyPreferences())
      }
      .map { prefs ->
        val raw = prefs[CUSTOM_GOALS_KEY] ?: return@map emptyList()
        try {
          json.decodeFromString<List<CustomGoalData>>(raw).map { it.toFastGoal() }
        } catch (e: Exception) {
          Log.e(LOG_TAG, "CustomGoalRepo: Error deserializing custom goals", e)
          emptyList()
        }
      }

  override suspend fun saveCustomGoal(goal: FastGoal, syncToRemote: Boolean) {
    try {
      dataStore.edit { prefs ->
        val existing = deserializeGoals(prefs)
        val updated = existing.filter { it.id != goal.id } + goal.toData()
        prefs[CUSTOM_GOALS_KEY] = json.encodeToString(updated)
      }
      Log.d(LOG_TAG, "CustomGoalRepo: Saved custom goal ${goal.id}")
      if (syncToRemote) {
        syncCustomGoalsToRemote()
      }
    } catch (e: Exception) {
      Log.e(LOG_TAG, "CustomGoalRepo: Error saving custom goal", e)
    }
  }

  override suspend fun deleteCustomGoal(goalId: String, syncToRemote: Boolean) {
    try {
      dataStore.edit { prefs ->
        val existing = deserializeGoals(prefs)
        val updated = existing.filter { it.id != goalId }
        prefs[CUSTOM_GOALS_KEY] = json.encodeToString(updated)
      }
      Log.d(LOG_TAG, "CustomGoalRepo: Deleted custom goal $goalId")
      if (syncToRemote) {
        syncCustomGoalsToRemote()
      }
    } catch (e: Exception) {
      Log.e(LOG_TAG, "CustomGoalRepo: Error deleting custom goal", e)
    }
  }

  override suspend fun replaceAllCustomGoals(goals: List<FastGoal>, syncToRemote: Boolean) {
    try {
      dataStore.edit { prefs ->
        val data = goals.map { it.toData() }
        prefs[CUSTOM_GOALS_KEY] = json.encodeToString(data)
      }
      Log.d(LOG_TAG, "CustomGoalRepo: Replaced all custom goals (count=${goals.size})")
      if (syncToRemote) {
        syncCustomGoalsToRemote()
      }
    } catch (e: Exception) {
      Log.e(LOG_TAG, "CustomGoalRepo: Error replacing custom goals", e)
    }
  }

  override suspend fun replaceAllCustomGoalsFromJson(goalsJson: String, syncToRemote: Boolean) {
    try {
      val goalDataList = json.decodeFromString<List<CustomGoalData>>(goalsJson)
      val goals = goalDataList.map { it.toFastGoal() }
      replaceAllCustomGoals(goals, syncToRemote)
    } catch (e: Exception) {
      Log.e(LOG_TAG, "CustomGoalRepo: Error replacing custom goals from JSON", e)
    }
  }

  private suspend fun syncCustomGoalsToRemote() {
    try {
      val prefs = dataStore.data.first()
      val goalsJson = prefs[CUSTOM_GOALS_KEY] ?: "[]"

      val request =
        PutDataMapRequest.create(DataLayerConstants.CUSTOM_GOALS_PATH)
          .apply {
            dataMap.putString(DataLayerConstants.CUSTOM_GOALS_JSON_KEY, goalsJson)
            dataMap.putLong(
              DataLayerConstants.CUSTOM_GOALS_TIMESTAMP_KEY,
              System.currentTimeMillis(),
            )
          }
          .asPutDataRequest()
          .setUrgent()

      dataClient.putDataItem(request).await()
      Log.d(LOG_TAG, "CustomGoalRepo: Custom goals synced to Data Layer")
    } catch (e: Exception) {
      Log.e(LOG_TAG, "CustomGoalRepo: Error syncing custom goals to Data Layer", e)
    }
  }

  private fun deserializeGoals(prefs: Preferences): List<CustomGoalData> {
    val raw = prefs[CUSTOM_GOALS_KEY] ?: return emptyList()
    return try {
      json.decodeFromString<List<CustomGoalData>>(raw)
    } catch (e: Exception) {
      Log.e(LOG_TAG, "CustomGoalRepo: Error deserializing goals", e)
      emptyList()
    }
  }

  companion object {
    private val CUSTOM_GOALS_KEY = stringPreferencesKey("custom_fasting_goals")
  }
}
