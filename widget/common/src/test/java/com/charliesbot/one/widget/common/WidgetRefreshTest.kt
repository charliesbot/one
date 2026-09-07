package com.charliesbot.one.widget.common

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test

class WidgetRefreshTest {
  private val repository = mockk<FastingDataRepository>()
  private val goals = mockk<GoalDurationResolver>()
  private val platform = mockk<WidgetPlatformAdapter>(relaxed = true)
  private val scheduler = WidgetRefreshScheduler(repository, goals, platform)
  private val hourMillis = 60L * 60L * 1000L

  @Before
  fun setup() {
    coEvery { platform.hasActiveWidgets() } returns true
  }

  @Test
  fun `missing fast cancels work before updating widgets`() = runTest {
    coEvery { repository.getCurrentFasting() } returns null

    scheduler.refresh()

    coVerifyOrder {
      platform.cancelScheduledWork()
      platform.requestWidgetUpdate()
    }
    coVerify(exactly = 0) { goals.durationMillis(any()) }
    verify(exactly = 0) { platform.enqueueDelayedWork(any(), any()) }
  }

  @Test
  fun `inactive fast cancels work before updating widgets`() = runTest {
    coEvery { repository.getCurrentFasting() } returns FastingDataItem(isFasting = false)

    scheduler.refresh()

    coVerifyOrder {
      platform.cancelScheduledWork()
      platform.requestWidgetUpdate()
    }
    verify(exactly = 0) { platform.enqueueDelayedWork(any(), any()) }
  }

  @Test
  fun `active fast resolves custom duration and updates before continuing`() = runTest {
    val fast =
      FastingDataItem(
        isFasting = true,
        startTimeInMillis = System.currentTimeMillis() - 5 * hourMillis,
        fastingGoalId = "custom",
      )
    coEvery { repository.getCurrentFasting() } returns fast
    coEvery { goals.durationMillis("custom") } returns 20 * hourMillis

    withTimeout(5000L) { scheduler.refresh() }

    coVerifyOrder {
      repository.getCurrentFasting()
      goals.durationMillis("custom")
      platform.requestWidgetUpdate()
      repository.getCurrentFasting()
      platform.enqueueDelayedWork(any(), replaceExisting = true)
    }
    coVerify(exactly = 1) { platform.requestWidgetUpdate() }
  }

  @Test
  fun `reached goal renders before ending the chain`() = runTest {
    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(
        isFasting = true,
        startTimeInMillis = System.currentTimeMillis() - 17 * hourMillis,
        fastingGoalId = "16:8",
      )
    coEvery { goals.durationMillis("16:8") } returns 16 * hourMillis

    scheduler.refresh()

    coVerifyOrder {
      platform.requestWidgetUpdate()
      platform.cancelScheduledWork()
    }
    verify(exactly = 0) { platform.enqueueDelayedWork(any(), any()) }
  }

  @Test
  fun `goal edited during update prevents stale continuation`() = runTest {
    var fast =
      FastingDataItem(
        isFasting = true,
        startTimeInMillis = System.currentTimeMillis() - 5 * hourMillis,
        fastingGoalId = "16:8",
      )
    coEvery { repository.getCurrentFasting() } coAnswers { fast }
    coEvery { goals.durationMillis("16:8") } returns 16 * hourMillis
    coEvery { platform.requestWidgetUpdate() } coAnswers
      {
        fast = fast.copy(fastingGoalId = "18:6")
      }

    scheduler.refresh()

    verify(exactly = 0) { platform.enqueueDelayedWork(any(), any()) }
    verify(exactly = 0) { platform.cancelScheduledWork() }
  }

  @Test(expected = IllegalStateException::class)
  fun `update failure propagates without scheduling continuation`() = runTest {
    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(
        isFasting = true,
        startTimeInMillis = System.currentTimeMillis(),
        fastingGoalId = "16:8",
      )
    coEvery { goals.durationMillis("16:8") } returns 16 * hourMillis
    coEvery { platform.requestWidgetUpdate() } throws IllegalStateException("Update failed")

    try {
      scheduler.refresh()
    } finally {
      verify(exactly = 0) { platform.enqueueDelayedWork(any(), any()) }
    }
  }

  @Test(expected = CancellationException::class)
  fun `cancellation propagates without scheduling continuation`() = runTest {
    coEvery { repository.getCurrentFasting() } throws CancellationException("Cancelled")

    try {
      scheduler.refresh()
    } finally {
      coVerify(exactly = 0) { platform.requestWidgetUpdate() }
      verify(exactly = 0) { platform.enqueueDelayedWork(any(), any()) }
    }
  }
}
