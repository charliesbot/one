package com.charliesbot.one.widget.common

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WidgetRefreshScheduler(
  private val fastingDataRepository: FastingDataRepository,
  private val goalDurationResolver: GoalDurationResolver,
  private val platformAdapter: WidgetPlatformAdapter,
) {
  private val mutex = Mutex()

  suspend fun onFastingStartedOrUpdated(
    fastingData: FastingDataItem,
    currentTimeMillis: Long = System.currentTimeMillis(),
  ) =
    mutex.withLock {
      if (!platformAdapter.hasActiveWidgets() || !fastingData.isFasting) {
        cancelLocked()
        return@withLock
      }
      val goalDuration = goalDurationResolver.durationMillis(fastingData.fastingGoalId)
      val delayMillis =
        WidgetRefreshCalculator.calculateNextRefreshDelayMillis(
          currentTimeMillis = currentTimeMillis,
          startTimeMillis = fastingData.startTimeInMillis,
          goalDurationMillis = goalDuration,
        )
      scheduleLocked(delayMillis = delayMillis, replaceExisting = true)
    }

  suspend fun onFastingCompleted() = mutex.withLock { cancelLocked() }

  suspend fun reconcile(currentTimeMillis: Long = System.currentTimeMillis()) =
    mutex.withLock {
      if (!platformAdapter.hasActiveWidgets()) {
        cancelLocked()
        return@withLock
      }

      val current = fastingDataRepository.getCurrentFasting()
      if (current == null || !current.isFasting) {
        cancelLocked()
        return@withLock
      }

      if (platformAdapter.hasActiveWork()) {
        // Preserve existing active work - do not cancel or postpone due refresh
        return@withLock
      }

      val goalDuration = goalDurationResolver.durationMillis(current.fastingGoalId)
      val delayMillis =
        WidgetRefreshCalculator.calculateNextRefreshDelayMillis(
          currentTimeMillis = currentTimeMillis,
          startTimeMillis = current.startTimeInMillis,
          goalDurationMillis = goalDuration,
        )

      if (delayMillis == null || delayMillis <= 0L) {
        // Fast has reached/passed goal while work was missing; update widget immediately
        platformAdapter.requestWidgetUpdate()
        cancelLocked()
        return@withLock
      }

      scheduleLocked(delayMillis = delayMillis, replaceExisting = false)
    }

  suspend fun onWorkerTickCompleted(
    snapshotStartTime: Long,
    snapshotGoalId: String,
    goalDurationMillis: Long,
    currentTimeMillis: Long = System.currentTimeMillis(),
  ) =
    mutex.withLock {
      if (!platformAdapter.hasActiveWidgets()) {
        cancelLocked()
        return@withLock
      }

      val current = fastingDataRepository.getCurrentFasting()
      if (current == null || !current.isFasting) {
        cancelLocked()
        return@withLock
      }

      // Protect against stale work: do not overwrite newer schedule if start time or goal changed
      if (
        current.startTimeInMillis != snapshotStartTime || current.fastingGoalId != snapshotGoalId
      ) {
        return@withLock
      }

      val delayMillis =
        WidgetRefreshCalculator.calculateNextRefreshDelayMillis(
          currentTimeMillis = currentTimeMillis,
          startTimeMillis = current.startTimeInMillis,
          goalDurationMillis = goalDurationMillis,
        )

      scheduleLocked(delayMillis = delayMillis, replaceExisting = true)
    }

  fun enqueueImmediateRecovery() {
    if (!platformAdapter.canEnqueueImmediateRecovery()) {
      cancel()
      return
    }

    platformAdapter.enqueueImmediateWork()
  }

  fun cancel() {
    platformAdapter.cancelScheduledWork()
  }

  private fun cancelLocked() {
    cancel()
  }

  private fun scheduleLocked(delayMillis: Long?, replaceExisting: Boolean) {
    if (delayMillis == null || delayMillis <= 0L) {
      cancelLocked()
      return
    }

    platformAdapter.enqueueDelayedWork(delayMillis = delayMillis, replaceExisting = replaceExisting)
  }
}
