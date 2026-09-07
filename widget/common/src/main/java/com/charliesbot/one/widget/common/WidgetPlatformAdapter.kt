package com.charliesbot.one.widget.common

/**
 * Widget discovery, background scheduling, and redraw operations used by [WidgetRefreshScheduler].
 *
 * Keeps Android APIs out of the shared scheduler. Implementations handle platform operations; the
 * scheduler decides when to request them. Enqueue and cancellation calls submit requests, rather
 * than waiting for background execution or cancellation to finish.
 */
interface WidgetPlatformAdapter {
  /**
   * Returns whether this platform currently has any installed instances of the managed widget.
   *
   * May suspend while querying the widget host. Does not enqueue work or request a redraw.
   */
  suspend fun hasActiveWidgets(): Boolean

  /**
   * Returns whether refresh work is queued or running for this adapter's schedule.
   *
   * Used to avoid replacing an existing schedule during reconciliation. The WorkManager adapter
   * returns false if the query fails, allowing a best-effort scheduling attempt, but propagates
   * coroutine cancellation.
   */
  suspend fun hasActiveWork(): Boolean

  /**
   * Requests a one-time refresh without an initial delay, keeping existing unfinished work.
   *
   * Does not make existing delayed work run sooner. If new work is enqueued, Android may still
   * defer execution; this call does not redraw widgets itself or wait for the worker to run.
   */
  fun enqueueImmediateWork()

  /**
   * Requests one future refresh after a minimum delay, rather than starting a repeating timer.
   *
   * For example, a delay of 40 minutes makes the refresh eligible after 40 minutes; it does not
   * guarantee execution at that exact time. The scheduler calculates this delay and decides whether
   * another refresh should follow after the worker runs.
   *
   * @param delayMillis Minimum wait before the new refresh can run, in milliseconds; nonnegative.
   * @param replaceExisting If true, cancels and replaces existing unfinished refresh work. If
   *   false, keeps existing unfinished work and enqueues this request only when no such work
   *   exists.
   */
  fun enqueueDelayedWork(delayMillis: Long, replaceExisting: Boolean)

  /**
   * Requests cancellation of this adapter's scheduled refresh work, including running work.
   *
   * Does not wait for cancellation to finish, change fasting data, or redraw widgets.
   */
  fun cancelScheduledWork()

  /**
   * Requests fresh content for the platform's widget instances, without scheduling future work.
   *
   * May suspend while the host processes the request. Returning does not guarantee the launcher or
   * watch has already displayed the updated content.
   */
  suspend fun requestWidgetUpdate()

  /**
   * Returns whether a non-suspending lifecycle callback should request an immediate refresh.
   *
   * This is a scheduling gate, not an Android permission or an execution-time guarantee. Phone
   * checks widget presence synchronously; Wear uses true and defers its suspending presence check
   * to worker execution. Unlike [hasActiveWidgets], true need not confirm a widget exists.
   */
  fun canRequestRefreshImmediately(): Boolean = true
}
