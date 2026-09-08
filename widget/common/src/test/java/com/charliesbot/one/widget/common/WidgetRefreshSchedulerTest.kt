package com.charliesbot.one.widget.common

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WidgetRefreshSchedulerTest {
  private val hour = 3600000L
  private var now = 10 * hour
  private var fast: FastingDataItem? =
    FastingDataItem(isFasting = true, startTimeInMillis = 0, fastingGoalId = "16:8")
  private var widgets = true
  private var scheduled = false
  private val repository = mockk<FastingDataRepository>()
  private val goals = mockk<GoalDurationResolver>()
  private val platform = mockk<WidgetPlatformAdapter>(relaxed = true)
  private val scheduler = WidgetRefreshScheduler(repository, goals, platform) { now }

  @Before
  fun setup() {
    coEvery { repository.getCurrentFasting() } coAnswers { fast }
    coEvery { goals.durationMillis(any()) } returns 16 * hour
    coEvery { platform.hasActiveWidgets() } coAnswers { widgets }
    every { platform.canRequestRefreshImmediately() } answers { widgets }
    every { platform.ensurePeriodicWork() } answers { scheduled = true }
    every { platform.cancelScheduledWork() } answers { scheduled = false }
  }

  @Test
  fun `repeated reconciliation ensures work without requesting redraws`() = runTest {
    repeat(3) { scheduler.reconcile() }
    assertTrue(scheduled)
    verify(exactly = 3) { platform.ensurePeriodicWork() }
    verify(exactly = 0) { platform.cancelScheduledWork() }
    coVerify(exactly = 0) { platform.requestWidgetUpdate() }
  }

  @Test
  fun `inactive missing and widgetless states cancel`() = runTest {
    fast = null
    scheduler.reconcile()
    fast = FastingDataItem(isFasting = false)
    scheduler.reconcile()
    fast = FastingDataItem(isFasting = true)
    widgets = false
    scheduler.reconcile()
    verify(exactly = 3) { platform.cancelScheduledWork() }
    verify(exactly = 0) { platform.ensurePeriodicWork() }
    coVerify(exactly = 0) { goals.durationMillis(any()) }
  }

  @Test
  fun `custom goal controls eligibility rather than a hardcoded duration`() = runTest {
    fast = fast!!.copy(fastingGoalId = "custom")
    coEvery { goals.durationMillis("custom") } returns 10 * hour + 500L
    scheduler.reconcile()
    assertTrue(scheduled)
    now += 500L
    scheduler.reconcile()
    assertFalse(scheduled)
    coVerifyOrder {
      platform.requestWidgetUpdate()
      platform.cancelScheduledWork()
    }
  }

  @Test
  fun `goal extension resumes work and shortening renders before cancellation`() = runTest {
    now = 17 * hour
    scheduler.reconcile()
    assertFalse(scheduled)
    fast = fast!!.copy(fastingGoalId = "18:6")
    coEvery { goals.durationMillis("18:6") } returns 18 * hour
    scheduler.reconcile()
    assertTrue(scheduled)
    fast = fast!!.copy(fastingGoalId = "16:8")
    scheduler.reconcile()
    assertFalse(scheduled)
  }

  @Test
  fun `late stop callback reconciles new persisted fast instead of cancelling it`() = runTest {
    fast = fast!!.copy(isFasting = false)
    scheduler.reconcile()
    fast = fast!!.copy(isFasting = true, startTimeInMillis = now)
    scheduler.reconcile() // New fast callback.
    scheduler.reconcile() // Delayed callback for the previous stop carries no stale payload.
    assertTrue(scheduled)
    verify(exactly = 1) { platform.cancelScheduledWork() }
  }

  @Test
  fun `inactive worker read cannot cancel a newer established schedule`() = runTest {
    val readEntered = CompletableDeferred<Unit>()
    val releaseRead = CompletableDeferred<Unit>()
    fast = fast!!.copy(isFasting = false)
    coEvery { repository.getCurrentFasting() } coAnswers
      {
        val snapshot = fast
        readEntered.complete(Unit)
        releaseRead.await()
        snapshot
      }
    val worker = launch { scheduler.refresh() }
    readEntered.await()
    fast = fast!!.copy(isFasting = true, startTimeInMillis = now)
    coEvery { repository.getCurrentFasting() } coAnswers { fast }
    val newStart = launch { scheduler.reconcile() }
    releaseRead.complete(Unit)
    worker.join()
    newStart.join()
    assertTrue(scheduled)
    coVerifyOrder {
      platform.cancelScheduledWork()
      platform.ensurePeriodicWork()
    }
  }

  @Test
  fun `clock is sampled after suspended reads`() = runTest {
    now = 15 * hour
    coEvery { goals.durationMillis(any()) } coAnswers
      {
        now = 17 * hour
        16 * hour
      }
    scheduler.reconcile()
    assertFalse(scheduled)
    coVerify(exactly = 1) { platform.requestWidgetUpdate() }
  }

  @Test
  fun `adding a widget provisions work even without a fast`() {
    fast = null
    scheduler.ensureRefreshEnqueued()
    assertTrue(scheduled)
    coVerify(exactly = 0) { repository.getCurrentFasting() }
  }

  @Test
  fun `removal cancels and readding restores provisioning`() {
    scheduler.ensureRefreshEnqueued()
    scheduler.cancel()
    assertFalse(scheduled)
    scheduler.ensureRefreshEnqueued()
    assertTrue(scheduled)
    widgets = false
    scheduler.ensureRefreshEnqueued()
    assertFalse(scheduled)
  }
}
