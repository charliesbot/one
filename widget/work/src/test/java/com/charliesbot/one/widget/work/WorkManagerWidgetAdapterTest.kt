package com.charliesbot.one.widget.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.charliesbot.one.widget.common.WidgetHost
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
  fun `periodic work repeats hourly with no initial delay and keeps existing work`() {
    val request = slot<PeriodicWorkRequest>()
    every {
      workManager.enqueueUniquePeriodicWork(
        workName,
        ExistingPeriodicWorkPolicy.KEEP,
        capture(request),
      )
    } returns mockk(relaxed = true)

    repeat(3) { adapter.ensurePeriodicWork() }

    assertEquals(WidgetRefreshWorker::class.java.name, request.captured.workSpec.workerClassName)
    assertEquals(3600000L, request.captured.workSpec.intervalDuration)
    assertEquals(0L, request.captured.workSpec.initialDelay)
    assertTrue(request.captured.workSpec.isPeriodic)
    assertFalse(request.captured.workSpec.hasConstraints())
    assertTrue(request.captured.tags.contains(workName))
    verify(exactly = 3) {
      workManager.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.KEEP, any())
    }
    verify(exactly = 0) { workManager.cancelUniqueWork(any()) }
  }

  @Test
  fun `cancellation uses the same unique schedule name`() {
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
