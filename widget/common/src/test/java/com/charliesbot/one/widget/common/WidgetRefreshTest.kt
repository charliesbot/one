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
import org.junit.Before
import org.junit.Test

class WidgetRefreshTest {
  private val hour = 3600000L
  private var fast: FastingDataItem? =
    FastingDataItem(isFasting = true, startTimeInMillis = 0, fastingGoalId = "16:8")
  private val repository = mockk<FastingDataRepository>()
  private val goals = mockk<GoalDurationResolver>()
  private val platform = mockk<WidgetPlatformAdapter>(relaxed = true)
  private val scheduler = WidgetRefreshScheduler(repository, goals, platform) { 10 * hour }

  @Before
  fun setup() {
    coEvery { platform.hasActiveWidgets() } returns true
    coEvery { repository.getCurrentFasting() } coAnswers { fast }
    coEvery { goals.durationMillis(any()) } returns 16 * hour
  }

  @Test
  fun `active refresh redraws without touching recurrence`() = runTest {
    repeat(3) { scheduler.refresh() }
    coVerify(exactly = 3) { platform.requestWidgetUpdate() }
    verify(exactly = 0) { platform.ensurePeriodicWork() }
    verify(exactly = 0) { platform.cancelScheduledWork() }
  }

  @Test
  fun `missing and inactive fast render before cancellation`() = runTest {
    for (state in listOf(null, FastingDataItem(isFasting = false))) {
      fast = state
      scheduler.refresh()
    }
    coVerifyOrder {
      platform.requestWidgetUpdate()
      platform.cancelScheduledWork()
      platform.requestWidgetUpdate()
      platform.cancelScheduledWork()
    }
    coVerify(exactly = 0) { goals.durationMillis(any()) }
  }

  @Test
  fun `goal reached redraws before cancelling periodic work`() = runTest {
    fast = fast!!.copy(startTimeInMillis = -7 * hour)
    scheduler.refresh()
    coVerifyOrder {
      platform.requestWidgetUpdate()
      platform.cancelScheduledWork()
    }
    verify(exactly = 0) { platform.ensurePeriodicWork() }
  }

  @Test
  fun `goal changed during redraw is evaluated from current state`() = runTest {
    coEvery { goals.durationMillis("custom") } returns 12 * hour
    coEvery { platform.requestWidgetUpdate() } coAnswers
      {
        fast = fast!!.copy(fastingGoalId = "custom")
      }
    scheduler.refresh()
    coVerify(exactly = 1) { goals.durationMillis("custom") }
    verify(exactly = 0) { platform.cancelScheduledWork() }
  }

  @Test
  fun `widgets removed during redraw stop periodic work`() = runTest {
    coEvery { platform.requestWidgetUpdate() } coAnswers
      {
        coEvery { platform.hasActiveWidgets() } returns false
      }
    scheduler.refresh()
    verify(exactly = 1) { platform.cancelScheduledWork() }
    coVerify(exactly = 0) { repository.getCurrentFasting() }
  }

  @Test
  fun `fast stopped during redraw stops periodic work`() = runTest {
    coEvery { platform.requestWidgetUpdate() } coAnswers { fast = fast!!.copy(isFasting = false) }
    scheduler.refresh()
    verify(exactly = 1) { platform.cancelScheduledWork() }
  }

  @Test(expected = IllegalStateException::class)
  fun `redraw failure propagates without cancelling recurrence`() = runTest {
    coEvery { platform.requestWidgetUpdate() } throws IllegalStateException("Update failed")
    try {
      scheduler.refresh()
    } finally {
      verify(exactly = 0) { platform.cancelScheduledWork() }
    }
  }

  @Test(expected = CancellationException::class)
  fun `eligibility cancellation propagates without cancelling recurrence`() = runTest {
    coEvery { repository.getCurrentFasting() } throws CancellationException("Cancelled")
    try {
      scheduler.refresh()
    } finally {
      verify(exactly = 0) { platform.cancelScheduledWork() }
    }
  }
}
