package com.charliesbot.one.widget.common

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Coordinates whether periodic widget work is needed; WorkManager owns repetition. */
class WidgetRefreshScheduler(
  private val fastingDataRepository: FastingDataRepository,
  private val goalDurationResolver: GoalDurationResolver,
  private val platformAdapter: WidgetPlatformAdapter,
  private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
  private val mutex = Mutex()

  /**
   * Requests current content, then cancels periodic work if it is no longer needed.
   *
   * Called by the worker. Redraws outside the mutex and reads eligibility afterward, so an old
   * pre-redraw snapshot cannot cancel a new fast. Does not enqueue a successor or reset recurrence.
   */
  suspend fun refresh() {
    platformAdapter.requestWidgetUpdate()
    mutex.withLock { if (refreshState() != RefreshState.ACTIVE) cancel() }
  }

  /**
   * Ensures or cancels periodic work using current persisted state, not an event's old payload.
   *
   * Called after local/synced fasting changes and when the main activity starts. Keeps existing
   * periodic work intact while eligible. At a reached goal, requests terminal content before
   * cancelling. This terminal redraw is under the mutex and must not reenter reconciliation.
   */
  suspend fun reconcile() =
    mutex.withLock {
      when (refreshState()) {
        RefreshState.ACTIVE -> platformAdapter.ensurePeriodicWork()
        RefreshState.GOAL_REACHED -> {
          platformAdapter.requestWidgetUpdate()
          cancel()
        }
        RefreshState.INACTIVE -> cancel()
      }
    }

  /**
   * Provisionally ensures periodic work when a widget is added/enabled, keeping existing work.
   *
   * No initial delay is requested, but Android may defer execution. The worker checks fasting
   * eligibility on its first run. The synchronous platform gate can reject requests when widget
   * presence is already known to be absent. Does not acquire the scheduling mutex.
   */
  fun ensureRefreshEnqueued() {
    if (platformAdapter.canRequestRefreshImmediately()) {
      platformAdapter.ensurePeriodicWork()
    } else {
      cancel()
    }
  }

  /**
   * Requests cancellation without acquiring the mutex or waiting for background cancellation.
   *
   * Used by non-suspending lifecycle callbacks, such as removal of the last phone widget. Fasting
   * callbacks use [reconcile] instead so a late stop event cannot blindly cancel a newer fast.
   */
  fun cancel() {
    platformAdapter.cancelScheduledWork()
  }

  // Called only under the mutex. Sample time after suspended reads, not before waiting for the
  // lock.
  private suspend fun refreshState(): RefreshState {
    if (!platformAdapter.hasActiveWidgets()) return RefreshState.INACTIVE
    val fast = fastingDataRepository.getCurrentFasting()
    if (fast == null || !fast.isFasting) return RefreshState.INACTIVE
    val duration = goalDurationResolver.durationMillis(fast.fastingGoalId)
    return if (currentTimeMillis() - fast.startTimeInMillis >= duration) {
      RefreshState.GOAL_REACHED
    } else {
      RefreshState.ACTIVE
    }
  }

  private enum class RefreshState {
    ACTIVE,
    GOAL_REACHED,
    INACTIVE,
  }
}
