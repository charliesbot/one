package com.charliesbot.shared.core.data.time

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.charliesbot.shared.core.data.repository.SettingsRepositoryImpl
import com.charliesbot.shared.core.models.TimeFormatMode
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import io.mockk.*
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

class TimeFormatSyncTest {
  @get:Rule val folder = TemporaryFolder()

  @Test
  fun `phone publishes persisted mode and revision and remote apply never echoes`() = runTest {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    mockkStatic(Wearable::class, PutDataMapRequest::class, Log::class)
    try {
      every { Log.d(any(), any()) } returns 0
      every { Log.e(any(), any(), any()) } returns 0
      val context = mockk<Context>()
      every { context.applicationContext } returns context
      val client = mockk<DataClient>()
      every { Wearable.getDataClient(context) } returns client
      val map = mockk<DataMap>(relaxed = true)
      val mapRequest = mockk<PutDataMapRequest>()
      val request = mockk<PutDataRequest>()
      every { PutDataMapRequest.create("/settings") } returns mapRequest
      every { mapRequest.dataMap } returns map
      every { mapRequest.asPutDataRequest() } returns request
      every { request.setUrgent() } returns request
      every { client.putDataItem(request) } returns Tasks.forResult(mockk<DataItem>())
      val file = folder.newFolder().resolve("sync.preferences_pb")
      val store = PreferenceDataStoreFactory.create(scope = scope) { file }
      val repo = SettingsRepositoryImpl(context, store)
      repo.setTimeFormatMode(TimeFormatMode.TWENTY_FOUR_HOUR)
      val revision = store.data.first()[TimeFormatPreferences.TIMESTAMP]!!
      assertTrue(revision > 0)
      assertEquals(TimeFormatMode.TWENTY_FOUR_HOUR, repo.timeFormatMode.first())
      verify { map.putString("time_format_mode", "TWENTY_FOUR_HOUR") }
      verify { map.putLong("time_format_timestamp", revision) }
      verify(exactly = 1) { client.putDataItem(request) }
      assertTrue(repo.applyRemoteTimeFormat(TimeFormatMode.SYSTEM, revision + 1))
      assertFalse(repo.applyRemoteTimeFormat(TimeFormatMode.TWELVE_HOUR, revision))
      verify(exactly = 1) { client.putDataItem(request) }
      assertEquals(TimeFormatMode.SYSTEM, repo.timeFormatMode.first())
    } finally {
      scope.cancel()
      unmockkStatic(Wearable::class, PutDataMapRequest::class, Log::class)
    }
  }
}
