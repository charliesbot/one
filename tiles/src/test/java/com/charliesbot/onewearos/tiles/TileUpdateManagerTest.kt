package com.charliesbot.onewearos.tiles

import android.content.Context
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TileUpdateManagerTest {

  private lateinit var mockContext: Context

  @Before
  fun setUp() {
    mockkStatic(Log::class)
    every { Log.d(any(), any()) } returns 0
    every { Log.e(any(), any()) } returns 0
    every { Log.e(any(), any(), any()) } returns 0
    mockContext = mockk(relaxed = true)
  }

  @Test
  fun `requestUpdate debounces multiple calls within time window`() {
    val testScope = TestScope()
    val dispatcher = UnconfinedTestDispatcher(testScope.testScheduler)
    var updateCount = 0
    val manager =
      TileUpdateManager(
        applicationContext = mockContext,
        timeWindow = 1000L,
        coroutineDispatcher = dispatcher,
        updater = { updateCount++ },
      )

    manager.requestUpdate()
    manager.requestUpdate()
    manager.requestUpdate()

    testScope.advanceTimeBy(500L)
    assertEquals("Should not have updated yet", 0, updateCount)

    testScope.advanceTimeBy(600L)
    assertEquals("Should have debounced to a single update", 1, updateCount)

    manager.cancel()
  }

  @Test
  fun `requestUpdate fires again after debounce window resets`() {
    val testScope = TestScope()
    val dispatcher = UnconfinedTestDispatcher(testScope.testScheduler)
    var updateCount = 0
    val manager =
      TileUpdateManager(
        applicationContext = mockContext,
        timeWindow = 1000L,
        coroutineDispatcher = dispatcher,
        updater = { updateCount++ },
      )

    manager.requestUpdate()
    testScope.advanceTimeBy(1100L)
    assertEquals(1, updateCount)

    manager.requestUpdate()
    testScope.advanceTimeBy(1100L)
    assertEquals(2, updateCount)

    manager.cancel()
  }
}
