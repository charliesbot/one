package com.charliesbot.one.widget.common

/** Platform operations for [WidgetRefreshScheduler], keeping Android APIs out of shared logic. */
interface WidgetPlatformAdapter {
  /**
   * Returns whether any instances of the managed widget are installed; may query a suspending host.
   */
  suspend fun hasActiveWidgets(): Boolean

  /**
   * Requests unique hourly periodic work with no initial delay, keeping unfinished work intact.
   *
   * Repeated calls do not reset its cadence. Android controls execution timing, which is not exact.
   * Returns after submitting the request, not after the worker runs. Does not redraw widgets
   * itself.
   */
  fun ensurePeriodicWork()

  /**
   * Requests cancellation of this schedule, including running work, without waiting for completion.
   */
  fun cancelScheduledWork()

  /**
   * Requests current widget content; returning does not guarantee the host has displayed it yet.
   */
  suspend fun requestWidgetUpdate()

  /**
   * Synchronous scheduling gate, not an Android permission. Phone checks widget presence now; Wear
   * permits the request and defers its suspending presence check to worker execution.
   */
  fun canRequestRefreshImmediately(): Boolean = true
}
