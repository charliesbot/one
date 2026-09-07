package com.charliesbot.one.widget.wear

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WearWidgetPlatformAdapterTest {
  private lateinit var context: Context
  private lateinit var workManager: WorkManager

  @Before
  fun setup() {
    context = mockk(relaxed = true)
    workManager = mockk(relaxed = true)
  }

  @Test
  fun `hasActiveWidgets delegates to activeWidgetChecker and canEnqueueImmediateRecovery is true`() =
    runTest {
      var active = true
      val adapter =
        WearWidgetPlatformAdapter(
          context = context,
          workManager = workManager,
          activeWidgetChecker = { active },
        )

      assertTrue(adapter.hasActiveWidgets())
      assertTrue(adapter.canEnqueueImmediateRecovery())

      active = false
      assertFalse(adapter.hasActiveWidgets())
      assertTrue(adapter.canEnqueueImmediateRecovery()) // Always true on Wear
    }

  @Test
  fun `hasActiveWork returns true when work is ENQUEUED or RUNNING`() = runTest {
    val adapter =
      WearWidgetPlatformAdapter(
        context = context,
        workManager = workManager,
        activeWidgetChecker = { true },
      )

    val activeWorkInfo = mockk<WorkInfo> { every { state } returns WorkInfo.State.ENQUEUED }
    val future =
      mockk<ListenableFuture<List<WorkInfo>>> {
        every { isDone } returns true
        every { get() } returns listOf(activeWorkInfo)
      }
    every { workManager.getWorkInfosForUniqueWork(WearWidgetPlatformAdapter.WORK_NAME) } returns
      future

    assertTrue(adapter.hasActiveWork())
  }

  @Test
  fun `hasActiveWork returns false when work list is empty`() = runTest {
    val adapter =
      WearWidgetPlatformAdapter(
        context = context,
        workManager = workManager,
        activeWidgetChecker = { true },
      )

    val emptyFuture =
      mockk<ListenableFuture<List<WorkInfo>>> {
        every { isDone } returns true
        every { get() } returns emptyList()
      }
    every { workManager.getWorkInfosForUniqueWork(WearWidgetPlatformAdapter.WORK_NAME) } returns
      emptyFuture

    assertFalse(adapter.hasActiveWork())
  }

  @Test
  fun `enqueueImmediateWork enqueues unique work with KEEP`() {
    val adapter =
      WearWidgetPlatformAdapter(
        context = context,
        workManager = workManager,
        activeWidgetChecker = { true },
      )

    adapter.enqueueImmediateWork()

    verify(exactly = 1) {
      workManager.enqueueUniqueWork(
        WearWidgetPlatformAdapter.WORK_NAME,
        ExistingWorkPolicy.KEEP,
        any<OneTimeWorkRequest>(),
      )
    }
  }

  @Test
  fun `enqueueDelayedWork with replaceExisting true enqueues with REPLACE and initialDelay`() {
    val adapter =
      WearWidgetPlatformAdapter(
        context = context,
        workManager = workManager,
        activeWidgetChecker = { true },
      )
    val enqueued = mutableListOf<OneTimeWorkRequest>()
    every {
      workManager.enqueueUniqueWork(
        WearWidgetPlatformAdapter.WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        capture(enqueued),
      )
    } returns mockk(relaxed = true)

    val delayMillis = 45 * 60 * 1000L
    adapter.enqueueDelayedWork(delayMillis = delayMillis, replaceExisting = true)

    assertEquals(1, enqueued.size)
    assertEquals(delayMillis, enqueued.first().workSpec.initialDelay)
  }

  @Test
  fun `enqueueDelayedWork with replaceExisting false enqueues with KEEP`() {
    val adapter =
      WearWidgetPlatformAdapter(
        context = context,
        workManager = workManager,
        activeWidgetChecker = { true },
      )

    adapter.enqueueDelayedWork(delayMillis = 3000L, replaceExisting = false)

    verify(exactly = 1) {
      workManager.enqueueUniqueWork(
        WearWidgetPlatformAdapter.WORK_NAME,
        ExistingWorkPolicy.KEEP,
        any<OneTimeWorkRequest>(),
      )
    }
  }

  @Test
  fun `cancelScheduledWork cancels unique work`() {
    val adapter =
      WearWidgetPlatformAdapter(
        context = context,
        workManager = workManager,
        activeWidgetChecker = { true },
      )

    adapter.cancelScheduledWork()

    verify(exactly = 1) { workManager.cancelUniqueWork(WearWidgetPlatformAdapter.WORK_NAME) }
  }

  @Test
  fun `requestWidgetUpdate invokes widget updater`() = runTest {
    var updateCalled = false
    val adapter =
      WearWidgetPlatformAdapter(
        context = context,
        workManager = workManager,
        activeWidgetChecker = { true },
        widgetUpdater = { updateCalled = true },
      )

    adapter.requestWidgetUpdate()

    assertTrue(updateCalled)
  }
}
