package com.charliesbot.shared.core.data.time

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.charliesbot.shared.core.domain.constants.DataLayerConstants.TIME_FORMAT_MODE_KEY
import com.charliesbot.shared.core.domain.constants.DataLayerConstants.TIME_FORMAT_TIMESTAMP_KEY
import com.charliesbot.shared.core.models.TimeFormatMode
import kotlinx.coroutines.flow.map

/** Atomically persists the mode and its revision so delayed sync cannot revert a selection. */
class TimeFormatPreferences(private val store: DataStore<Preferences>) {
  val mode = store.data.map { TimeFormatMode.fromStoredValue(it[MODE]) }

  suspend fun set(mode: TimeFormatMode, now: Long) {
    store.edit {
      it[MODE] = mode.name
      it[TIMESTAMP] = maxOf(now, (it[TIMESTAMP] ?: 0L) + 1L)
    }
  }

  suspend fun applyRemote(mode: TimeFormatMode, timestamp: Long): Boolean {
    var applied = false
    store.edit {
      if (timestamp > (it[TIMESTAMP] ?: 0L)) {
        it[MODE] = mode.name
        it[TIMESTAMP] = timestamp
        applied = true
      }
    }
    return applied
  }

  companion object {
    val MODE = stringPreferencesKey(TIME_FORMAT_MODE_KEY)
    val TIMESTAMP = longPreferencesKey(TIME_FORMAT_TIMESTAMP_KEY)
  }
}
