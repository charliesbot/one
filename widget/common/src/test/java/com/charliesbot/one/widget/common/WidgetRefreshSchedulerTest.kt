package com.charliesbot.one.widget.common

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WidgetRefreshSchedulerTest {
  private lateinit var repository: FastingDataRepository
  private lateinit var goalDurationResolver: GoalDurationResolver
  private lateinit var platformAdapter: WidgetPlatformAdapter

  private val oneHourMillis = 60L * 60L * 1000L

  @Before
  fun setup() {
    repository = mockk(relaxed = true)
    goalDurationResolver = mockk()
    platformAdapter = mockk(relaxed = true)

    coEvery { platformAdapter.hasActiveWidgets() } returns true
    coEvery { platformAdapter.hasActiveWork() } returns false
    coEvery { platformAdapter.canEnqueueImmediateRecovery() } returns true
  }

  // --- 1. Delayed Execution ---

  @Test
  fun `onFastingStartedOrUpdated schedules delayed work with replaceExisting true`() = runTest {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
    val startTime = 1000L
    val currentTime = startTime + (2 * oneHourMillis) + (15 * 60 * 1000L) // 2h 15m elapsed
    val goalId = "16:8"
    val goalDuration = 16 * oneHourMillis
    coEvery { goalDurationResolver.durationMillis(goalId) } returns goalDuration

    scheduler.onFastingStartedOrUpdated(
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = goalId),
      currentTimeMillis = currentTime,
    )

    // Next hour boundary is 3h elapsed -> delay is 45 minutes
    val expectedDelay = 45 * 60 * 1000L
    verify(exactly = 1) {
      platformAdapter.enqueueDelayedWork(delayMillis = expectedDelay, replaceExisting = true)
    }
  }

  @Test
  fun `onWorkerTickCompleted schedules next boundary when fast is still ongoing`() = runTest {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
    val startTime = 1000L
    val goalDuration = 16 * oneHourMillis
    val currentTime = startTime + (5 * oneHourMillis) + (10 * 60 * 1000L) // 5h 10m elapsed

    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = "16:8")

    scheduler.onWorkerTickCompleted(
      snapshotStartTime = startTime,
      snapshotGoalId = "16:8",
      goalDurationMillis = goalDuration,
      currentTimeMillis = currentTime,
    )

    // Next hour boundary is 6h elapsed -> delay is 50 minutes
    val expectedDelay = 50 * 60 * 1000L
    verify(exactly = 1) {
      platformAdapter.enqueueDelayedWork(delayMillis = expectedDelay, replaceExisting = true)
    }
  }

  // --- 2. Custom Goals ---

  @Test
  fun `custom goal duration drives delayed execution and completion calculation`() = runTest {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
    val startTime = 1000L
    val customGoalId = "custom_14h"
    val customDuration = 14 * oneHourMillis
    val currentTime = startTime + (13 * oneHourMillis) + (20 * 60 * 1000L) // 13h 20m elapsed

    coEvery { goalDurationResolver.durationMillis(customGoalId) } returns customDuration

    scheduler.onFastingStartedOrUpdated(
      FastingDataItem(
        isFasting = true,
        startTimeInMillis = startTime,
        fastingGoalId = customGoalId,
      ),
      currentTimeMillis = currentTime,
    )

    // Next hour boundary is 14h (goal reached) -> delay is 40 minutes
    val expectedDelay = 40 * 60 * 1000L
    verify(exactly = 1) {
      platformAdapter.enqueueDelayedWork(delayMillis = expectedDelay, replaceExisting = true)
    }
  }

  // --- 3. Goal Edits & Stale Worker Checks ---

  @Test
  fun `in-flight worker tick with stale goal does not overwrite newer goal schedule`() = runTest {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
    val startTime = 1000L
    val oldGoalId = "16:8"
    val newGoalId = "18:6"
    val oldGoalDuration = 16 * oneHourMillis
    val newGoalDuration = 18 * oneHourMillis
    val currentTime = startTime + (10 * oneHourMillis) + (15 * 60 * 1000L) // 10h 15m elapsed

    coEvery { goalDurationResolver.durationMillis(oldGoalId) } returns oldGoalDuration
    coEvery { goalDurationResolver.durationMillis(newGoalId) } returns newGoalDuration

    val capturedDelays = mutableListOf<Long>()
    every {
      platformAdapter.enqueueDelayedWork(capture(capturedDelays), replaceExisting = true)
    } returns Unit

    // 1. Goal edit to 18:6 occurs and is persisted
    val newFasting =
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = newGoalId)
    coEvery { repository.getCurrentFasting() } returns newFasting

    scheduler.onFastingStartedOrUpdated(newFasting, currentTimeMillis = currentTime)
    assertEquals(1, capturedDelays.size)
    val expectedDelay = 45 * 60 * 1000L
    assertEquals(expectedDelay, capturedDelays.last())

    // 2. In-flight worker tick from old 16:8 goal finishes afterwards
    scheduler.onWorkerTickCompleted(
      snapshotStartTime = startTime,
      snapshotGoalId = oldGoalId,
      goalDurationMillis = oldGoalDuration,
      currentTimeMillis = currentTime,
    )

    // Worker tick drops due to stale goalId without rescheduling or cancelling
    verify(exactly = 0) { platformAdapter.cancelScheduledWork() }
    assertEquals(1, capturedDelays.size)
    assertEquals(expectedDelay, capturedDelays.last())
  }

  @Test
  fun `goal edit after worker tick replaces schedule with newer goal timing`() = runTest {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
    val startTime = 1000L
    val oldGoalId = "16:8"
    val newGoalId = "18:6"
    val oldGoalDuration = 16 * oneHourMillis
    val newGoalDuration = 18 * oneHourMillis
    val currentTime = startTime + (15 * oneHourMillis) + (30 * 60 * 1000L) // 15h 30m elapsed

    coEvery { goalDurationResolver.durationMillis(oldGoalId) } returns oldGoalDuration
    coEvery { goalDurationResolver.durationMillis(newGoalId) } returns newGoalDuration

    val capturedDelays = mutableListOf<Long>()
    every {
      platformAdapter.enqueueDelayedWork(capture(capturedDelays), replaceExisting = true)
    } returns Unit

    // 1. Worker tick completes when fast is still on old 16:8 goal (delay to 16h boundary = 30m)
    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = oldGoalId)

    scheduler.onWorkerTickCompleted(
      snapshotStartTime = startTime,
      snapshotGoalId = oldGoalId,
      goalDurationMillis = oldGoalDuration,
      currentTimeMillis = currentTime,
    )
    assertEquals(1, capturedDelays.size)
    assertEquals(30 * 60 * 1000L, capturedDelays[0])

    // 2. User edits goal to 18:6 at 16h elapsed
    val editTime = startTime + oldGoalDuration
    val newFasting =
      FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = newGoalId)
    coEvery { repository.getCurrentFasting() } returns newFasting

    scheduler.onFastingStartedOrUpdated(newFasting, currentTimeMillis = editTime)
    assertEquals(2, capturedDelays.size)
    assertEquals(oneHourMillis, capturedDelays.last())
  }

  @Test
  fun `onWorkerTickCompleted drops tick when start time changed during execution`() = runTest {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(isFasting = true, startTimeInMillis = 5000L, fastingGoalId = "16:8")

    scheduler.onWorkerTickCompleted(
      snapshotStartTime = 1000L,
      snapshotGoalId = "16:8",
      goalDurationMillis = 16 * oneHourMillis,
      currentTimeMillis = 2000L,
    )

    verify(exactly = 0) { platformAdapter.enqueueDelayedWork(any(), any()) }
  }

  // --- 4. Cancellation ---

  @Test
  fun `onFastingStartedOrUpdated cancels when no active widgets exist`() = runTest {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
    coEvery { platformAdapter.hasActiveWidgets() } returns false

    scheduler.onFastingStartedOrUpdated(
      FastingDataItem(isFasting = true, startTimeInMillis = 1000L, fastingGoalId = "16:8")
    )

    verify(exactly = 1) { platformAdapter.cancelScheduledWork() }
    verify(exactly = 0) { platformAdapter.enqueueDelayedWork(any(), any()) }
  }

  @Test
  fun `onFastingStartedOrUpdated cancels when isFasting is false`() = runTest {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)

    scheduler.onFastingStartedOrUpdated(
      FastingDataItem(isFasting = false, startTimeInMillis = 1000L, fastingGoalId = "16:8")
    )

    verify(exactly = 1) { platformAdapter.cancelScheduledWork() }
  }

  @Test
  fun `onFastingCompleted cancels scheduled work`() = runTest {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)

    scheduler.onFastingCompleted()

    verify(exactly = 1) { platformAdapter.cancelScheduledWork() }
  }

  @Test
  fun `onWorkerTickCompleted cancels when goal is reached`() = runTest {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
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

    verify(exactly = 1) { platformAdapter.cancelScheduledWork() }
    verify(exactly = 0) { platformAdapter.enqueueDelayedWork(any(), any()) }
  }

  @Test
  fun `onWorkerTickCompleted cancels when no widgets remain`() = runTest {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
    coEvery { platformAdapter.hasActiveWidgets() } returns false

    scheduler.onWorkerTickCompleted(
      snapshotStartTime = 1000L,
      snapshotGoalId = "16:8",
      goalDurationMillis = 16 * oneHourMillis,
      currentTimeMillis = 2000L,
    )

    verify(exactly = 1) { platformAdapter.cancelScheduledWork() }
    verify(exactly = 0) { platformAdapter.enqueueDelayedWork(any(), any()) }
  }

  @Test
  fun `cancel delegates to platformAdapter`() {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)

    scheduler.cancel()

    verify(exactly = 1) { platformAdapter.cancelScheduledWork() }
  }

  // --- 5. Recovery ---

  @Test
  fun `enqueueImmediateRecovery enqueues immediate work when canEnqueueImmediateRecovery is true`() {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
    coEvery { platformAdapter.canEnqueueImmediateRecovery() } returns true

    scheduler.enqueueImmediateRecovery()

    verify(exactly = 1) { platformAdapter.enqueueImmediateWork() }
    verify(exactly = 0) { platformAdapter.cancelScheduledWork() }
  }

  @Test
  fun `enqueueImmediateRecovery cancels work when canEnqueueImmediateRecovery is false`() {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
    coEvery { platformAdapter.canEnqueueImmediateRecovery() } returns false

    scheduler.enqueueImmediateRecovery()

    verify(exactly = 1) { platformAdapter.cancelScheduledWork() }
    verify(exactly = 0) { platformAdapter.enqueueImmediateWork() }
  }

  @Test
  fun `reconcile preserves existing active work without altering it`() = runTest {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
    coEvery { platformAdapter.hasActiveWork() } returns true
    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(isFasting = true, startTimeInMillis = 1000L, fastingGoalId = "16:8")

    scheduler.reconcile()

    verify(exactly = 0) { platformAdapter.enqueueDelayedWork(any(), any()) }
    verify(exactly = 0) { platformAdapter.cancelScheduledWork() }
  }

  @Test
  fun `reconcile establishes missing work with replaceExisting false when fast is active`() =
    runTest {
      val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
      val startTime = System.currentTimeMillis() - (2 * oneHourMillis)
      coEvery { platformAdapter.hasActiveWork() } returns false
      coEvery { repository.getCurrentFasting() } returns
        FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = "16:8")
      coEvery { goalDurationResolver.durationMillis("16:8") } returns 16 * oneHourMillis

      scheduler.reconcile()

      verify(exactly = 1) { platformAdapter.enqueueDelayedWork(any(), replaceExisting = false) }
    }

  // --- 6. Terminal Rendering ---

  @Test
  fun `reconcile requests widget update and cancels when goal has already passed while work was missing`() =
    runTest {
      val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
      val startTime = 1000L
      val goalDuration = 16 * oneHourMillis
      val currentTime = startTime + (17 * oneHourMillis) // 17h elapsed > 16h goal

      coEvery { platformAdapter.hasActiveWork() } returns false
      coEvery { repository.getCurrentFasting() } returns
        FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = "16:8")
      coEvery { goalDurationResolver.durationMillis("16:8") } returns goalDuration

      scheduler.reconcile(currentTimeMillis = currentTime)

      coVerify(exactly = 1) { platformAdapter.requestWidgetUpdate() }
      verify(exactly = 1) { platformAdapter.cancelScheduledWork() }
      verify(exactly = 0) { platformAdapter.enqueueDelayedWork(any(), any()) }
    }

  // --- 7. Controlled Suspension: Conflicting Operations ---

  @Test
  fun `controlled suspension verifies mutex serializes conflicting operations`() = runTest {
    val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
    val gate = CompletableDeferred<Unit>()
    var reconcileEntered = false
    var startFastingExecuted = false

    // Suspend inside hasActiveWidgets during reconcile
    coEvery { platformAdapter.hasActiveWidgets() } coAnswers
      {
        reconcileEntered = true
        gate.await()
        true
      }
    coEvery { repository.getCurrentFasting() } returns
      FastingDataItem(isFasting = true, startTimeInMillis = 1000L, fastingGoalId = "16:8")
    coEvery { goalDurationResolver.durationMillis("16:8") } returns 16 * oneHourMillis

    // Launch operation 1: reconcile
    val job1 = launch { scheduler.reconcile() }
    runCurrent()
    assertTrue(reconcileEntered)

    // Operation 2: onFastingStartedOrUpdated launched concurrently
    // Reconfigure hasActiveWidgets to return true immediately for the second call
    coEvery { platformAdapter.hasActiveWidgets() } returns true
    val job2 = launch {
      scheduler.onFastingStartedOrUpdated(
        FastingDataItem(isFasting = true, startTimeInMillis = 2000L, fastingGoalId = "16:8")
      )
      startFastingExecuted = true
    }
    runCurrent()

    // Mutex ensures operation 2 is blocked while operation 1 is suspended
    assertFalse(startFastingExecuted)

    // Release operation 1
    gate.complete(Unit)
    runCurrent()

    job1.join()
    job2.join()

    // Now operation 2 has completed after operation 1
    assertTrue(startFastingExecuted)
  }

  @Test
  fun `controlled suspension verifies in-flight worker tick detects concurrent goal edit`() =
    runTest {
      val scheduler = WidgetRefreshScheduler(repository, goalDurationResolver, platformAdapter)
      val startTime = 1000L
      val oldGoalId = "16:8"
      val newGoalId = "18:6"
      val oldGoalDuration = 16 * oneHourMillis
      val newGoalDuration = 18 * oneHourMillis
      val currentTime = startTime + (10 * oneHourMillis) + (15 * 60 * 1000L)

      coEvery { goalDurationResolver.durationMillis(oldGoalId) } returns oldGoalDuration
      coEvery { goalDurationResolver.durationMillis(newGoalId) } returns newGoalDuration

      val capturedDelays = mutableListOf<Long>()
      every {
        platformAdapter.enqueueDelayedWork(capture(capturedDelays), replaceExisting = true)
      } returns Unit

      val workerReadGate = CompletableDeferred<Unit>()
      var currentFasting: FastingDataItem =
        FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = oldGoalId)

      // Worker suspends while reading repository
      coEvery { repository.getCurrentFasting() } coAnswers
        {
          workerReadGate.await()
          currentFasting
        }

      // Launch worker tick
      val workerJob = launch {
        scheduler.onWorkerTickCompleted(
          snapshotStartTime = startTime,
          snapshotGoalId = oldGoalId,
          goalDurationMillis = oldGoalDuration,
          currentTimeMillis = currentTime,
        )
      }
      runCurrent()

      // While worker is suspended, user edits goal and saves to repository
      currentFasting =
        FastingDataItem(isFasting = true, startTimeInMillis = startTime, fastingGoalId = newGoalId)

      // Release worker
      workerReadGate.complete(Unit)
      workerJob.join()

      // Worker detected that repository's fastingGoalId ("18:6") != snapshotGoalId ("16:8")
      // Therefore it dropped the tick without rescheduling or cancelling
      verify(exactly = 0) { platformAdapter.cancelScheduledWork() }
      assertEquals(0, capturedDelays.size)
    }
}
