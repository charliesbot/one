package com.charliesbot.shared.core.data.repositories.customGoalRepository

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.FastGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CustomGoalRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : CustomGoalRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override val customGoals: Flow<List<FastGoal>> = dataStore.data
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

    override suspend fun saveCustomGoal(goal: FastGoal) {
        try {
            dataStore.edit { prefs ->
                val existing = deserializeGoals(prefs)
                val updated = existing.filter { it.id != goal.id } + goal.toData()
                prefs[CUSTOM_GOALS_KEY] = json.encodeToString(updated)
            }
            Log.d(LOG_TAG, "CustomGoalRepo: Saved custom goal ${goal.id}")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "CustomGoalRepo: Error saving custom goal", e)
        }
    }

    override suspend fun deleteCustomGoal(goalId: String) {
        try {
            dataStore.edit { prefs ->
                val existing = deserializeGoals(prefs)
                val updated = existing.filter { it.id != goalId }
                prefs[CUSTOM_GOALS_KEY] = json.encodeToString(updated)
            }
            Log.d(LOG_TAG, "CustomGoalRepo: Deleted custom goal $goalId")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "CustomGoalRepo: Error deleting custom goal", e)
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

private fun CustomGoalData.toFastGoal(): FastGoal = FastGoal(
    id = id,
    titleText = name,
    durationDisplay = formatDurationDisplay(durationMillis),
    color = Color(colorHex.toULong()),
    durationMillis = durationMillis,
)

private fun FastGoal.toData(): CustomGoalData = CustomGoalData(
    id = id,
    name = titleText ?: id,
    durationMillis = durationMillis,
    colorHex = color.value.toLong(),
)

private fun formatDurationDisplay(millis: Long): String {
    val totalHours = millis / (60L * 60L * 1000L)
    return totalHours.toString()
}
