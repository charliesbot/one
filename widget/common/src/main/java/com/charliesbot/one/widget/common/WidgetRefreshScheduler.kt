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

  /**
   * Requests a widget redraw and decides whether another background refresh is needed.
   *
   * Called by the refresh worker. Reads a fasting snapshot before requesting the redraw, then uses
   * [onWorkerTickCompleted] to avoid overwriting a schedule for a changed fast. The redraw is
   * outside the scheduling mutex so other scheduling operations can proceed while it suspends. If
   * fasting is inactive or missing, cancels scheduled work and requests a redraw without continuing
   * the chain.
   */
  suspend fun refresh() {
    val snapshot = fastingDataRepository.getCurrentFasting()
    if (snapshot == null || !snapshot.isFasting) {
      onFastingCompleted()
      platformAdapter.requestWidgetUpdate()
      return
    }

    val goalDuration = goalDurationResolver.durationMillis(snapshot.fastingGoalId)
    // Updating may suspend while fasting state changes; do not hold the scheduling mutex here.
    platformAdapter.requestWidgetUpdate()
    onWorkerTickCompleted(
      snapshotStartTime = snapshot.startTimeInMillis,
      snapshotGoalId = snapshot.fastingGoalId,
      goalDurationMillis = goalDuration,
    )
  }

  /**
   * Replaces the next refresh schedule after a local or synced fasting start or update.
   *
   * Checks widget presence, resolves the goal duration, and calculates the next delay while holding
   * the scheduling mutex. Cancels work if there are no widgets, fasting is inactive, or the goal
   * has been reached. Does not redraw widgets; the event caller handles the immediate UI update.
   *
   * @param fastingData The fasting state supplied by the event callback.
   * @param currentTimeMillis Time used to calculate the next refresh delay, in epoch milliseconds.
   */
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

  /**
   * Requests cancellation of scheduled refreshes when a fasting stop/completion event is received.
   *
   * Acquires the scheduling mutex to serialize this request with other protected scheduling
   * operations. Does not change fasting data or redraw widgets.
   */
  suspend fun onFastingCompleted() = mutex.withLock { cancelLocked() }

  /**
   * Restores missing refresh work when the main activity starts, preserving queued or running work.
   *
   * Cancels scheduling if widgets or an active fast are absent. Otherwise, if work is missing,
   * schedules the next refresh; if the goal has already been reached, requests a redraw and cancels
   * scheduling instead. Holds the scheduling mutex throughout, including that goal-reached redraw.
   *
   * @param currentTimeMillis Time used to calculate the next refresh delay, in epoch milliseconds.
   */
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

  /**
   * Schedules the next refresh after a worker has requested a widget redraw.
   *
   * Rechecks widget presence and current fasting state under the scheduling mutex. Cancels work if
   * no widgets or active fast remain. If the start time or goal ID differs from the pre-redraw
   * snapshot, leaves scheduling untouched so an older refresh cannot replace the newer schedule.
   * Otherwise, replaces the next refresh request, or cancels it when the goal has been reached.
   *
   * @param snapshotStartTime Fast start time read before the redraw, in epoch milliseconds.
   * @param snapshotGoalId Goal ID read before the redraw.
   * @param goalDurationMillis Goal duration resolved for that snapshot, in milliseconds.
   * @param currentTimeMillis Time used to calculate the next refresh delay, in epoch milliseconds.
   */
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

  /**
   * Ensures refresh work is queued when a widget is added or enabled, keeping existing work intact.
   *
   * "Recovery" means restoring a potentially missing refresh schedule, not recovering fasting data.
   * Requests work without an initial delay; Android may still defer execution. Uses the platform's
   * synchronous recovery gate: phone checks widget presence now, while Wear allows the request and
   * defers presence checking to worker execution. Cancels work if the gate rejects the request.
   *
   * Does not acquire the scheduling mutex or wait for the worker to run, so non-suspending
   * lifecycle callbacks can invoke it directly.
   */
  fun enqueueImmediateRecovery() {
    if (!platformAdapter.canEnqueueImmediateRecovery()) {
      cancel()
      return
    }

    platformAdapter.enqueueImmediateWork()
  }

  /**
   * Requests cancellation of refresh work without acquiring the scheduling mutex.
   *
   * Used by synchronous lifecycle callbacks, such as removal of the last phone widget. Does not
   * change fasting data or redraw widgets. Unlike [onFastingCompleted], this call is not serialized
   * with protected scheduling operations and does not wait for background cancellation to finish.
   */
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
