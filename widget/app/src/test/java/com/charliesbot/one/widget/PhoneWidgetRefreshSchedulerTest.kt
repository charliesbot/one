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
  fun `onFastingStartedOrUpdated schedules with REPLACE when widgets active and fasting`() = runTest {
    val scheduler = PhoneWidgetRefreshScheduler(
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
        PhoneWidgetRefreshScheduler.WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        any<OneTimeWorkRequest>(),
      )
    }
  }

  @Test
  fun `onFastingStartedOrUpdated cancels when no active widgets exist`() = runTest {
    val scheduler = PhoneWidgetRefreshScheduler(
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
    verify(exactly = 0) {
      workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
    }
  }

  @Test
  fun `reconcile preserves existing ENQUEUED work without modifying it`() = runTest {
    val scheduler = PhoneWidgetRefreshScheduler(
      context = context,
      fastingDataRepository = repository,
      goalResolver = goalResolver,
      workManager = workManager,
      activeWidgetChecker = { true },
    )
    val activeWorkInfo = mockk<WorkInfo> {
      every { state } returns WorkInfo.State.ENQUEUED
    }
    val future = mockk<ListenableFuture<List<WorkInfo>>> {
      every { get() } returns listOf(activeWorkInfo)
    }
    every { workManager.getWorkInfosForUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) } returns future
    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(isFasting = true, startTimeInMillis = 1000L, fastingGoalId = "16:8")

    scheduler.reconcile()

    // Must NOT enqueue or cancel anything
    verify(exactly = 0) {
      workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
    }
    verify(exactly = 0) {
      workManager.cancelUniqueWork(any())
    }
  }

  @Test
  fun `reconcile establishes missing work with KEEP when fast is active`() = runTest {
    val scheduler = PhoneWidgetRefreshScheduler(
      context = context,
      fastingDataRepository = repository,
      goalResolver = goalResolver,
      workManager = workManager,
      activeWidgetChecker = { true },
    )
    val emptyFuture = mockk<ListenableFuture<List<WorkInfo>>> {
      every { get() } returns emptyList()
    }
    every { workManager.getWorkInfosForUniqueWork(PhoneWidgetRefreshScheduler.WORK_NAME) } returns emptyFuture
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
        PhoneWidgetRefreshScheduler.WORK_NAME,
        ExistingWorkPolicy.KEEP,
        any<OneTimeWorkRequest>(),
      )
    }
  }

  @Test
  fun `onWorkerTickCompleted protects against stale work when fasting state changed during execution`() = runTest {
    val scheduler = PhoneWidgetRefreshScheduler(
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
    verify(exactly = 0) {
      workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
    }
  }

  @Test
  fun `onWorkerTickCompleted cancels when goal is reached`() = runTest {
    val scheduler = PhoneWidgetRefreshScheduler(
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
    verify(exactly = 0) {
      workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>())
    }
  }

  @Test
  fun `onWorkerTickCompleted schedules next boundary when fast is still ongoing`() = runTest {
    val scheduler = PhoneWidgetRefreshScheduler(
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
}
