package com.charliesbot.one.widget.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.charliesbot.one.widget.common.WidgetHost
import com.google.common.util.concurrent.ListenableFuture
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkManagerWidgetAdapterTest {
  private val context = mockk<Context>(relaxed = true)
  private val workManager = mockk<WorkManager>(relaxed = true)
  private val host = mockk<WidgetHost>(relaxed = true)
  private val workName = "test_widget_refresh"
  private val adapter = WorkManagerWidgetAdapter(context, workName, host, workManager)

  @Test
  fun `immediate work keeps existing work and uses the shared worker`() {
    val request = slot<OneTimeWorkRequest>()
    every {
      workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, capture(request))
    } returns mockk(relaxed = true)

    adapter.enqueueImmediateWork()

    assertEquals(WidgetRefreshWorker::class.java.name, request.captured.workSpec.workerClassName)
    assertEquals(0L, request.captured.workSpec.initialDelay)
    assertTrue(request.captured.tags.contains(workName))
  }

  @Test
  fun `delayed work maps both policies and preserves the configured delay`() {
    for (replace in listOf(false, true)) {
      val policy = if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
      val request = slot<OneTimeWorkRequest>()
      every { workManager.enqueueUniqueWork(workName, policy, capture(request)) } returns
        mockk(relaxed = true)

      adapter.enqueueDelayedWork(45000L, replace)

      assertEquals(45000L, request.captured.workSpec.initialDelay)
      assertEquals(WidgetRefreshWorker::class.java.name, request.captured.workSpec.workerClassName)
      assertTrue(request.captured.tags.contains(workName))
    }
  }

  @Test
  fun `only enqueued and running work count as active`() = runTest {
    for (workState in WorkInfo.State.entries) {
      val info = mockk<WorkInfo> { every { state } returns workState }
      val future =
        mockk<ListenableFuture<List<WorkInfo>>> {
          every { isDone } returns true
          every { get() } returns listOf(info)
        }
      every { workManager.getWorkInfosForUniqueWork(workName) } returns future

      assertEquals(
        workState == WorkInfo.State.ENQUEUED || workState == WorkInfo.State.RUNNING,
        adapter.hasActiveWork(),
      )
    }
  }

  @Test
  fun `empty work query allows recovery`() = runTest {
    val future =
      mockk<ListenableFuture<List<WorkInfo>>> {
        every { isDone } returns true
        every { get() } returns emptyList()
      }
    every { workManager.getWorkInfosForUniqueWork(workName) } returns future
    assertFalse(adapter.hasActiveWork())
  }

  @Test
  fun `query failure preserves best effort recovery`() = runTest {
    every { workManager.getWorkInfosForUniqueWork(workName) } throws
      IllegalStateException("Unavailable")
    assertFalse(adapter.hasActiveWork())
  }

  @Test(expected = CancellationException::class)
  fun `query cancellation propagates`() = runTest {
    every { workManager.getWorkInfosForUniqueWork(workName) } throws
      CancellationException("Cancelled")
    adapter.hasActiveWork()
  }

  @Test
  fun `cancellation uses the configured unique work name`() {
    adapter.cancelScheduledWork()
    verify(exactly = 1) { workManager.cancelUniqueWork(workName) }
  }

  @Test
  fun `widget operations delegate to the platform host`() = runTest {
    coEvery { host.hasActiveWidgets() } returns true
    every { host.canRequestRefreshImmediately() } returns false

    assertTrue(adapter.hasActiveWidgets())
    assertFalse(adapter.canRequestRefreshImmediately())
    adapter.requestWidgetUpdate()

    coVerify(exactly = 1) { host.requestWidgetUpdate() }
  }
}
