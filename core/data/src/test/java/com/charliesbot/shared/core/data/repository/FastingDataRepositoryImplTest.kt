package com.charliesbot.shared.core.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import com.charliesbot.shared.core.constants.PredefinedFastingGoals
import com.charliesbot.shared.core.models.FastingDataItem
import com.google.android.gms.wearable.Wearable
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FastingDataRepositoryImplTest {

  @Before
  fun setup() {
    mockkStatic(Log::class)
    every { Log.d(any(), any()) } returns 0
    every { Log.e(any(), any()) } returns 0
    every { Log.e(any(), any(), any()) } returns 0
    mockkStatic(Wearable::class)
    every { Wearable.getDataClient(any<Context>()) } returns mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `unreadable store still emits the default fasting state`() = runTest {
    val unreadable =
      mockk<DataStore<Preferences>> {
        every { data } returns flow { throw IOException("disk read failed") }
      }
    val repository = FastingDataRepositoryImpl(mockk(relaxed = true), unreadable)

    assertEquals(
      listOf(
        FastingDataItem(
          isFasting = false,
          startTimeInMillis = -1,
          updateTimestamp = -1,
          fastingGoalId = PredefinedFastingGoals.SIXTEEN_EIGHT.id,
        )
      ),
      repository.fastingDataItem.toList(),
    )
  }
}
