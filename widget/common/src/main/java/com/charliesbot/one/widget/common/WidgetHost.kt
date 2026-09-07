package com.charliesbot.one.widget.common

/** Platform widget discovery and rendering, independent of background scheduling. */
interface WidgetHost {
  suspend fun hasActiveWidgets(): Boolean

  suspend fun requestWidgetUpdate()

  /** Synchronous recovery gate; hosts needing suspend discovery defer that check to the worker. */
  fun canRequestRefreshImmediately(): Boolean = true
}
