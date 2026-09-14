package com.charliesbot.shared.core.data.time

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.charliesbot.shared.core.models.TimeFormatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TimeFormatPreferencesTest {
  @get:Rule val folder = TemporaryFolder()

  @Test
  fun `default unknown values and explicit selection survive reopening`() = runTest {
    val file = folder.newFolder().resolve("clock.preferences_pb")
    val job = SupervisorJob()
    val store =
      PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { file }
    val prefs = TimeFormatPreferences(store)
    assertEquals(TimeFormatMode.SYSTEM, prefs.mode.first())
    store.edit { it[TimeFormatPreferences.MODE] = "future-mode" }
    assertEquals(TimeFormatMode.SYSTEM, prefs.mode.first())
    prefs.set(TimeFormatMode.TWENTY_FOUR_HOUR, 100)
    job.cancel()
    job.join()
    val reopenedScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    try {
      val reopened = PreferenceDataStoreFactory.create(scope = reopenedScope) { file }
      assertEquals(TimeFormatMode.TWENTY_FOUR_HOUR, TimeFormatPreferences(reopened).mode.first())
    } finally {
      reopenedScope.cancel()
    }
  }

  @Test
  fun `remote revisions reject stale duplicate and absent timestamps`() = runTest {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    try {
      val file = folder.newFolder().resolve("clock.preferences_pb")
      val store = PreferenceDataStoreFactory.create(scope = scope) { file }
      val prefs = TimeFormatPreferences(store)
      assertFalse(prefs.applyRemote(TimeFormatMode.TWELVE_HOUR, 0))
      assertTrue(prefs.applyRemote(TimeFormatMode.TWENTY_FOUR_HOUR, 200))
      assertFalse(prefs.applyRemote(TimeFormatMode.TWELVE_HOUR, 100))
      assertFalse(prefs.applyRemote(TimeFormatMode.TWELVE_HOUR, 200))
      assertEquals(TimeFormatMode.TWENTY_FOUR_HOUR, prefs.mode.first())
      assertTrue(prefs.applyRemote(TimeFormatMode.SYSTEM, 201))
      assertEquals(TimeFormatMode.SYSTEM, prefs.mode.first())
      prefs.set(TimeFormatMode.TWELVE_HOUR, 100)
      assertEquals(202L, store.data.first()[TimeFormatPreferences.TIMESTAMP])
    } finally {
      scope.cancel()
    }
  }
}
