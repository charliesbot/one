package com.charliesbot.shared.core.datastore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.fastingDataStore by preferencesDataStore(name = "fasting_state_prefs")