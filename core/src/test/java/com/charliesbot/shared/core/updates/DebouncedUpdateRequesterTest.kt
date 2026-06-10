package com.charliesbot.shared.core.updates

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebouncedUpdateRequesterTest {
  @Test
  fun `requestUpdate runs update after debounce window`() = runTest {
    var updateCount = 0
    val requester =
      DebouncedUpdateRequester(
        name = REQUESTER_NAME,
        timeWindow = DEBOUNCE_WINDOW,
        coroutineDispatcher = StandardTestDispatcher(testScheduler),
        update = { updateCount++ },
        logInitialized = {},
        logUpdateFailure = {},
      )
    advanceUntilIdle()

    requester.requestUpdate()
    advanceTimeBy(DEBOUNCE_WINDOW)
    advanceUntilIdle()

    assertEquals(1, updateCount)
    requester.cancel()
  }

  @Test
  fun `requestUpdate collapses multiple requests during debounce window`() = runTest {
    var updateCount = 0
    val requester =
      DebouncedUpdateRequester(
        name = REQUESTER_NAME,
        timeWindow = DEBOUNCE_WINDOW,
        coroutineDispatcher = StandardTestDispatcher(testScheduler),
        update = { updateCount++ },
        logInitialized = {},
        logUpdateFailure = {},
      )
    advanceUntilIdle()

    requester.requestUpdate()
    advanceTimeBy(DEBOUNCE_WINDOW / 2)
    requester.requestUpdate()
    advanceTimeBy(DEBOUNCE_WINDOW / 2)
    requester.requestUpdate()
    advanceTimeBy(DEBOUNCE_WINDOW)
    advanceUntilIdle()

    assertEquals(1, updateCount)
    requester.cancel()
  }

  @Test
  fun `requestUpdate logs update failures`() = runTest {
    val error = IllegalStateException("update failed")
    var loggedError: Throwable? = null
    val requester =
      DebouncedUpdateRequester(
        name = REQUESTER_NAME,
        timeWindow = DEBOUNCE_WINDOW,
        coroutineDispatcher = StandardTestDispatcher(testScheduler),
        update = { throw error },
        logInitialized = {},
        logUpdateFailure = { loggedError = it },
      )
    advanceUntilIdle()

    requester.requestUpdate()
    advanceTimeBy(DEBOUNCE_WINDOW)
    advanceUntilIdle()

    assertSame(error, loggedError)
    requester.cancel()
  }

  private companion object {
    private const val REQUESTER_NAME = "TestUpdateRequester"
    private const val DEBOUNCE_WINDOW = 1_000L
  }
}
