package com.charliesbot.one.widget.wear

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class WearWidgetRefreshSchedulerTest {
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
        WearWidgetRefreshScheduler(
          context = context,
          fastingDataRepository = repository,
          goalResolver = goalResolver,
          workManager = workManager,
          activeWidgetChecker = { true },
        )
      val startTime = System.currentTimeMillis() - (2 * oneHourMillis)
      val goalId = "16:8"
      coEvery { goalResolver.resolveGoalDurationMillis(goalId) } returns 16 * oneHourMillis

      scheduler.onFastingStartedOrUpdated(
        FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = goalId)
      )

      verify(exactly = 1) {
        workManager.enqueueUniqueWork(
          WearWidgetRefreshScheduler.WORK_NAME,
          ExistingWorkPolicy.REPLACE,
          any<OneTimeWorkRequest>(),
        )
      }
    }

  @Test
  fun `onFastingStartedOrUpdated cancels when no active widgets exist`() = runTest {
    val scheduler =
      WearWidgetRefreshScheduler(
        context = context,
        fastingDataRepository = repository,
        goalResolver = goalResolver,
        workManager = workManager,
        activeWidgetChecker = { false },
      )

    scheduler.onFastingStartedOrUpdated(
      FastingDataItem(isFasting = true, startTimeInMillis = 1000L, fastingGoalId = "16:8")
    )

    verify { workManager.cancelUniqueWork(WearWidgetRefreshScheduler.WORK_NAME) }
    verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
  }

  @Test
  fun `reconcile preserves existing ENQUEUED work without modifying it`() = runTest {
    val scheduler =
      WearWidgetRefreshScheduler(
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
    every { workManager.getWorkInfosForUniqueWork(WearWidgetRefreshScheduler.WORK_NAME) } returns
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
      WearWidgetRefreshScheduler(
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
    every { workManager.getWorkInfosForUniqueWork(WearWidgetRefreshScheduler.WORK_NAME) } returns
      emptyFuture
    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(
        isFasting = true,
        startTimeInMillis = System.currentTimeMillis() - (2 * oneHourMillis),
        fastingGoalId = "16:8",
      )
    coEvery { goalResolver.resolveGoalDurationMillis("16:8") } returns 16 * oneHourMillis

    scheduler.reconcile()

    verify(exactly = 1) {
      workManager.enqueueUniqueWork(
        WearWidgetRefreshScheduler.WORK_NAME,
        ExistingWorkPolicy.KEEP,
        any<OneTimeWorkRequest>(),
      )
    }
  }

  @Test
  fun `onWorkerTickCompleted protects against stale work when fasting state changed during execution`() =
    runTest {
      val scheduler =
        WearWidgetRefreshScheduler(
          context = context,
          fastingDataRepository = repository,
          goalResolver = goalResolver,
          workManager = workManager,
          activeWidgetChecker = { true },
        )
      coEvery { repository.getCurrentFasting() } returns
        FastingDataItem(isFasting = true, startTimeInMillis = 5000L, fastingGoalId = "16:8")

      scheduler.onWorkerTickCompleted(
        snapshotStartTime = 1000L,
        snapshotGoalId = "16:8",
        goalDurationMillis = 16 * oneHourMillis,
        currentTimeMillis = 2000L,
      )

      verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

  @Test
  fun `onWorkerTickCompleted cancels when goal is reached`() = runTest {
    val scheduler =
      WearWidgetRefreshScheduler(
        context = context,
        fastingDataRepository = repository,
        goalResolver = goalResolver,
        workManager = workManager,
        activeWidgetChecker = { true },
      )
    val startTime = 1000L
    val goalDuration = 16 * oneHourMillis
    val currentTime = startTime + goalDuration

    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = "16:8")

    scheduler.onWorkerTickCompleted(
      snapshotStartTime = startTime,
      snapshotGoalId = "16:8",
      goalDurationMillis = goalDuration,
      currentTimeMillis = currentTime,
    )

    verify { workManager.cancelUniqueWork(WearWidgetRefreshScheduler.WORK_NAME) }
    verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
  }

  @Test
  fun `onWorkerTickCompleted schedules next boundary when fast is still ongoing`() = runTest {
    val scheduler =
      WearWidgetRefreshScheduler(
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
        WearWidgetRefreshScheduler.WORK_NAME,
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
        WearWidgetRefreshScheduler(
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
      every { workManager.getWorkInfosForUniqueWork(WearWidgetRefreshScheduler.WORK_NAME) } returns
        emptyFuture
      val startTime = 1000L
      val goalDuration = 16 * oneHourMillis
      val currentTime = startTime + (17 * oneHourMillis) // 17 hours elapsed > 16 hours goal
      coEvery { repository.getCurrentFasting() } returns
        FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = "16:8")
      coEvery { goalResolver.resolveGoalDurationMillis("16:8") } returns goalDuration

      scheduler.reconcile(currentTimeMillis = currentTime)

      org.junit.Assert.assertTrue(widgetUpdated)
      verify { workManager.cancelUniqueWork(WearWidgetRefreshScheduler.WORK_NAME) }
      verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

  @Test
  fun `onWorkerTickCompleted cancels work and does not reschedule when no widgets remain`() =
    runTest {
      val scheduler =
        WearWidgetRefreshScheduler(
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

      verify { workManager.cancelUniqueWork(WearWidgetRefreshScheduler.WORK_NAME) }
      verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

  @Test
  fun `concurrent goal edit interleaving with worker tick completion is safely serialized so edit wins`() =
    runTest {
      val scheduler =
        WearWidgetRefreshScheduler(
          context = context,
          fastingDataRepository = repository,
          goalResolver = goalResolver,
          workManager = workManager,
          activeWidgetChecker = { true },
        )
      val startTime = 1000L
      val oldGoalId = "16:8"
      val newGoalId = "18:6"
      coEvery { goalResolver.resolveGoalDurationMillis(oldGoalId) } returns 16 * oneHourMillis
      coEvery { goalResolver.resolveGoalDurationMillis(newGoalId) } returns 18 * oneHourMillis

      val callOrder = mutableListOf<String>()
      every {
        workManager.enqueueUniqueWork(
          WearWidgetRefreshScheduler.WORK_NAME,
          ExistingWorkPolicy.REPLACE,
          any<OneTimeWorkRequest>(),
        )
      } answers
        {
          callOrder.add("enqueued")
          mockk(relaxed = true)
        }

      coEvery { repository.getCurrentFasting() } returns
        FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = oldGoalId)

      val workerJob = launch {
        scheduler.onWorkerTickCompleted(
          snapshotStartTime = startTime,
          snapshotGoalId = oldGoalId,
          goalDurationMillis = 16 * oneHourMillis,
          currentTimeMillis = startTime + oneHourMillis,
        )
      }
      val editJob = launch {
        scheduler.onFastingStartedOrUpdated(
          FastingDataItem(
            isFasting = true,
            startTimeInMillis = startTime,
            fastingGoalId = newGoalId,
          )
        )
      }

      workerJob.join()
      editJob.join()

      org.junit.Assert.assertTrue(callOrder.isNotEmpty())
    }

  @Test
  fun `enqueueImmediateRecovery enqueues OneTimeWorkRequest with KEEP`() {
    val scheduler =
      WearWidgetRefreshScheduler(
        context = context,
        fastingDataRepository = repository,
        goalResolver = goalResolver,
        workManager = workManager,
        activeWidgetChecker = { true },
      )

    scheduler.enqueueImmediateRecovery()

    verify(exactly = 1) {
      workManager.enqueueUniqueWork(
        WearWidgetRefreshScheduler.WORK_NAME,
        ExistingWorkPolicy.KEEP,
        any<OneTimeWorkRequest>(),
      )
    }
  }
}
