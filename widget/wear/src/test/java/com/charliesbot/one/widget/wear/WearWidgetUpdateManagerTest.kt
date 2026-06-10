package com.charliesbot.one.widget.wear

import android.content.Context
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearWidgetUpdateManagerTest {
  private val context: Context = mockk(relaxed = true)

  @Before
  fun setUp() {
    mockkStatic(Log::class)
    every { Log.d(any(), any()) } returns 0
    every { Log.e(any(), any(), any()) } returns 0
  }

  @After
  fun tearDown() {
    unmockkStatic(Log::class)
  }

  @Test
  fun `requestUpdate triggers widget update after debounce window`() = runTest {
    var updateCount = 0
    val manager =
      WearWidgetUpdateManager(
        applicationContext = context,
        timeWindow = DEBOUNCE_WINDOW,
        coroutineDispatcher = StandardTestDispatcher(testScheduler),
        updateWidgets = { updateCount++ },
      )
    advanceUntilIdle()

    manager.requestUpdate()

    advanceTimeBy(DEBOUNCE_WINDOW)
    advanceUntilIdle()

    assertEquals(1, updateCount)
    manager.cancel()
  }

  @Test
  fun `requestUpdate collapses multiple requests during debounce window`() = runTest {
    var updateCount = 0
    val manager =
      WearWidgetUpdateManager(
        applicationContext = context,
        timeWindow = DEBOUNCE_WINDOW,
        coroutineDispatcher = StandardTestDispatcher(testScheduler),
        updateWidgets = { updateCount++ },
      )
    advanceUntilIdle()

    manager.requestUpdate()
    advanceTimeBy(DEBOUNCE_WINDOW / 2)
    manager.requestUpdate()
    advanceTimeBy(DEBOUNCE_WINDOW / 2)
    manager.requestUpdate()
    advanceTimeBy(DEBOUNCE_WINDOW)
    advanceUntilIdle()

    assertEquals(1, updateCount)
    manager.cancel()
  }

  private companion object {
    private const val DEBOUNCE_WINDOW = 1_000L
  }
}
