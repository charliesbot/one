package com.charliesbot.one.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.utils.GoalResolver
import com.google.common.util.concurrent.ListenableFuture
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PhoneWidgetRefreshSchedulerTest {
  private lateinit var context: Context
  private lateinit var workManager: WorkManager
  private lateinit var repository: FastingDataRepository
  private lateinit var goalResolver: GoalResolver
  private val oneHourMillis = 60L * 60L * 1000L

  @Before
  fun setup() {
    context = mockk(relaxed = true)
    workManager = mockk(relaxed = true)
    repository = mockk()
    goalResolver = mockk()
  }

  @Test
  fun `onFastingStartedOrUpdated schedules with REPLACE when widgets active and fasting`() =
    runTest {
      val scheduler =
        PhoneWidgetRefreshScheduler(
          context = context,
          fastingDataRepository = repository,
          goalResolver = goalResolver,
          workManager = workManager,
          activeWidgetChecker = { true },
        )
      val startTime = System.currentTimeMillis() - (2 * oneHourMillis)
      val goalId = "16:8"
      coEvery { goalResolver.durationMillis(goalId) } returns 16 * oneHourMillis

      scheduler.onFastingStartedOrUpdated(
        FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = goalId)
      )

      verify(exactly = 1) {
        workManager.enqueueUniqueWork(
          PhoneWidgetRefreshScheduler.WORK_NAME,
          ExistingWorkPolicy.REPLACE,
          any<OneTimeWorkRequest>(),
        )
      }
    }

  @Test
  fun `onFastingStartedOrUpdated cancels when no active widgets exist`() = runTest {
    val scheduler =
      PhoneWidgetRefreshScheduler(
        context = context,
        fastingDataRepository = repository,
        goalResolver = goalResolver,
        workManager = workManager,
        activeWidgetChecker = { false },
      )

    scheduler.onFastingStartedOrUpdated(
      FastingDataItem(isFasting = true, startTimeInMillis = 1000L, fastingGoalId = "16:8")
    )

    verify { workManager.cancelUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) }
    verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
  }

  @Test
  fun `reconcile preserves existing ENQUEUED work without modifying it`() = runTest {
    val scheduler =
      PhoneWidgetRefreshScheduler(
        context = context,
        fastingDataRepository = repository,
        goalResolver = goalResolver,
        workManager = workManager,
        activeWidgetChecker = { true },
      )
    val activeWorkInfo = mockk<WorkInfo> { every { state } returns WorkInfo.State.ENQUEUED }
    val future =
      mockk<ListenableFuture<List<WorkInfo>>> {
        every { isDone } returns true
        every { get() } returns listOf(activeWorkInfo)
      }
    every { workManager.getWorkInfosForUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) } returns
      future
    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(isFasting = true, startTimeInMillis = 1000L, fastingGoalId = "16:8")

    scheduler.reconcile()

    // Must NOT enqueue or cancel anything
    verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    verify(exactly = 0) { workManager.cancelUniqueWork(any()) }
  }

  @Test
  fun `reconcile establishes missing work with KEEP when fast is active`() = runTest {
    val scheduler =
      PhoneWidgetRefreshScheduler(
        context = context,
        fastingDataRepository = repository,
        goalResolver = goalResolver,
        workManager = workManager,
        activeWidgetChecker = { true },
      )
    val emptyFuture =
      mockk<ListenableFuture<List<WorkInfo>>> {
        every { isDone } returns true
        every { get() } returns emptyList()
      }
    every { workManager.getWorkInfosForUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) } returns
      emptyFuture
    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(
        isFasting = true,
        startTimeInMillis = System.currentTimeMillis() - (2 * oneHourMillis),
        fastingGoalId = "16:8",
      )
    coEvery { goalResolver.durationMillis("16:8") } returns 16 * oneHourMillis

    scheduler.reconcile()

    verify(exactly = 1) {
      workManager.enqueueUniqueWork(
        PhoneWidgetRefreshScheduler.WORK_NAME,
        ExistingWorkPolicy.KEEP,
        any<OneTimeWorkRequest>(),
      )
    }
  }

  @Test
  fun `onWorkerTickCompleted protects against stale work when fasting state changed during execution`() =
    runTest {
      val scheduler =
        PhoneWidgetRefreshScheduler(
          context = context,
          fastingDataRepository = repository,
          goalResolver = goalResolver,
          workManager = workManager,
          activeWidgetChecker = { true },
        )
      // Fast was updated with a new start time while worker was executing
      coEvery { repository.getCurrentFasting() } returns
        FastingDataItem(isFasting = true, startTimeInMillis = 5000L, fastingGoalId = "16:8")

      scheduler.onWorkerTickCompleted(
        snapshotStartTime = 1000L, // old start time
        snapshotGoalId = "16:8",
        goalDurationMillis = 16 * oneHourMillis,
        currentTimeMillis = 2000L,
      )

      // Must not schedule or cancel because newer schedule was already enqueued by edit
      verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

  @Test
  fun `onWorkerTickCompleted cancels when goal is reached`() = runTest {
    val scheduler =
      PhoneWidgetRefreshScheduler(
        context = context,
        fastingDataRepository = repository,
        goalResolver = goalResolver,
        workManager = workManager,
        activeWidgetChecker = { true },
      )
    val startTime = 1000L
    val goalDuration = 16 * oneHourMillis
    val currentTime = startTime + goalDuration // exactly goal reached

    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = "16:8")

    scheduler.onWorkerTickCompleted(
      snapshotStartTime = startTime,
      snapshotGoalId = "16:8",
      goalDurationMillis = goalDuration,
      currentTimeMillis = currentTime,
    )

    verify { workManager.cancelUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) }
    verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
  }

  @Test
  fun `onWorkerTickCompleted schedules next boundary when fast is still ongoing`() = runTest {
    val scheduler =
      PhoneWidgetRefreshScheduler(
        context = context,
        fastingDataRepository = repository,
        goalResolver = goalResolver,
        workManager = workManager,
        activeWidgetChecker = { true },
      )
    val startTime = 1000L
    val goalDuration = 16 * oneHourMillis
    val currentTime = startTime + (5 * oneHourMillis) + (15 * 60 * 1000L)

    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = "16:8")

    scheduler.onWorkerTickCompleted(
      snapshotStartTime = startTime,
      snapshotGoalId = "16:8",
      goalDurationMillis = goalDuration,
      currentTimeMillis = currentTime,
    )

    verify(exactly = 1) {
      workManager.enqueueUniqueWork(
        PhoneWidgetRefreshScheduler.WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        any<OneTimeWorkRequest>(),
      )
    }
  }

  @Test
  fun `reconcile triggers immediate widget update and cancels when goal has already passed`() =
    runTest {
      var widgetUpdated = false
      val scheduler =
        PhoneWidgetRefreshScheduler(
          context = context,
          fastingDataRepository = repository,
          goalResolver = goalResolver,
          workManager = workManager,
          activeWidgetChecker = { true },
          widgetUpdater = { widgetUpdated = true },
        )
      val emptyFuture =
        mockk<ListenableFuture<List<WorkInfo>>> {
          every { isDone } returns true
          every { get() } returns emptyList()
        }
      every { workManager.getWorkInfosForUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) } returns
        emptyFuture
      val startTime = 1000L
      val goalDuration = 16 * oneHourMillis
      val currentTime = startTime + (17 * oneHourMillis) // 17 hours elapsed > 16 hours goal
      coEvery { repository.getCurrentFasting() } returns
        FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = "16:8")
      coEvery { goalResolver.durationMillis("16:8") } returns goalDuration

      withTimeout(5000L) { scheduler.reconcile(currentTimeMillis = currentTime) }

      org.junit.Assert.assertTrue(widgetUpdated)
      verify { workManager.cancelUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) }
      verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

  @Test
  fun `onWorkerTickCompleted cancels work and does not reschedule when no widgets remain`() =
    runTest {
      val scheduler =
        PhoneWidgetRefreshScheduler(
          context = context,
          fastingDataRepository = repository,
          goalResolver = goalResolver,
          workManager = workManager,
          activeWidgetChecker = { false },
        )

      scheduler.onWorkerTickCompleted(
        snapshotStartTime = 1000L,
        snapshotGoalId = "16:8",
        goalDurationMillis = 16 * oneHourMillis,
        currentTimeMillis = 2000L,
      )

      verify { workManager.cancelUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) }
      verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

  @Test
  fun `in-flight worker tick with stale goal does not overwrite newer goal schedule`() = runTest {
    val scheduler =
      PhoneWidgetRefreshScheduler(
        context = context,
        fastingDataRepository = repository,
        goalResolver = goalResolver,
        workManager = workManager,
        activeWidgetChecker = { true },
      )
    val startTime = 1000L
    val oldGoalId = "16:8"
    val newGoalId = "18:6"
    val oldGoalDuration = 16 * oneHourMillis
    val newGoalDuration = 18 * oneHourMillis
    val currentTime = startTime + (10 * oneHourMillis) + (15 * 60 * 1000L) // 10h 15m elapsed

    coEvery { goalResolver.durationMillis(oldGoalId) } returns oldGoalDuration
    coEvery { goalResolver.durationMillis(newGoalId) } returns newGoalDuration

    val enqueuedRequests = mutableListOf<OneTimeWorkRequest>()
    every {
      workManager.enqueueUniqueWork(
        PhoneWidgetRefreshScheduler.WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        capture(enqueuedRequests),
      )
    } returns mockk(relaxed = true)

    // 1. Goal edit to 18:6 occurs and is persisted
    val newFasting =
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = newGoalId)
    coEvery { repository.getCurrentFasting() } returns newFasting

    // Edit schedules refresh: from 10h 15m elapsed to 11h boundary = 45m delay
    scheduler.onFastingStartedOrUpdated(newFasting, currentTimeMillis = currentTime)
    assertEquals(1, enqueuedRequests.size)
    val expectedDelay = 45 * 60 * 1000L
    assertEquals(expectedDelay, enqueuedRequests.last().workSpec.initialDelay)

    // 2. In-flight worker tick from old 16:8 goal finishes afterwards
    scheduler.onWorkerTickCompleted(
      snapshotStartTime = startTime,
      snapshotGoalId = oldGoalId,
      goalDurationMillis = oldGoalDuration,
      currentTimeMillis = currentTime,
    )

    // Worker detects stale goal, drops tick without cancelling work, so edit's request survives
    verify(exactly = 0) { workManager.cancelUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) }
    assertEquals(1, enqueuedRequests.size)
    assertEquals(expectedDelay, enqueuedRequests.last().workSpec.initialDelay)
  }

  @Test
  fun `goal edit after worker tick replaces schedule with newer goal timing`() = runTest {
    val scheduler =
      PhoneWidgetRefreshScheduler(
        context = context,
        fastingDataRepository = repository,
        goalResolver = goalResolver,
        workManager = workManager,
        activeWidgetChecker = { true },
      )
    val startTime = 1000L
    val oldGoalId = "16:8"
    val newGoalId = "18:6"
    val oldGoalDuration = 16 * oneHourMillis
    val newGoalDuration = 18 * oneHourMillis
    val currentTime = startTime + (15 * oneHourMillis) + (30 * 60 * 1000L) // 15h 30m elapsed

    coEvery { goalResolver.durationMillis(oldGoalId) } returns oldGoalDuration
    coEvery { goalResolver.durationMillis(newGoalId) } returns newGoalDuration

    val enqueuedRequests = mutableListOf<OneTimeWorkRequest>()
    every {
      workManager.enqueueUniqueWork(
        PhoneWidgetRefreshScheduler.WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        capture(enqueuedRequests),
      )
    } returns mockk(relaxed = true)

    // 1. Worker tick completes when fast is still on old 16:8 goal (delay to 16h boundary = 30m)
    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = oldGoalId)

    scheduler.onWorkerTickCompleted(
      snapshotStartTime = startTime,
      snapshotGoalId = oldGoalId,
      goalDurationMillis = oldGoalDuration,
      currentTimeMillis = currentTime,
    )
    assertEquals(1, enqueuedRequests.size)
    assertEquals(30 * 60 * 1000L, enqueuedRequests[0].workSpec.initialDelay)

    // 2. User edits goal to 18:6 at 16h elapsed (at which point 16:8 would have finished, but 18:6
    // has 1h delay)
    val editTime = startTime + oldGoalDuration // 16h elapsed
    val newFasting =
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = newGoalId)
    coEvery { repository.getCurrentFasting() } returns newFasting

    scheduler.onFastingStartedOrUpdated(newFasting, currentTimeMillis = editTime)
    assertEquals(2, enqueuedRequests.size)
    // The newer goal's schedule replaces the worker's schedule with next hour timing
    assertEquals(oneHourMillis, enqueuedRequests.last().workSpec.initialDelay)
  }

  @Test
  fun `enqueueImmediateRecovery enqueues OneTimeWorkRequest with KEEP`() = runTest {
    val scheduler =
      PhoneWidgetRefreshScheduler(
        context = context,
        fastingDataRepository = repository,
        goalResolver = goalResolver,
        workManager = workManager,
        activeWidgetChecker = { true },
      )

    scheduler.enqueueImmediateRecovery()

    verify(exactly = 1) {
      workManager.enqueueUniqueWork(
        PhoneWidgetRefreshScheduler.WORK_NAME,
        ExistingWorkPolicy.KEEP,
        any<OneTimeWorkRequest>(),
      )
    }
  }

  @Test
  fun `enqueueImmediateRecovery cancels work when no active widgets exist`() = runTest {
    val scheduler =
      PhoneWidgetRefreshScheduler(
        context = context,
        fastingDataRepository = repository,
        goalResolver = goalResolver,
        workManager = workManager,
        activeWidgetChecker = { false },
      )

    scheduler.enqueueImmediateRecovery()

    verify { workManager.cancelUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) }
    verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
  }
}
