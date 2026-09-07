package com.charliesbot.one.widget.common

interface WidgetPlatformAdapter {
  suspend fun hasActiveWidgets(): Boolean

  suspend fun hasActiveWork(): Boolean

  fun enqueueImmediateWork()

  fun enqueueDelayedWork(delayMillis: Long, replaceExisting: Boolean)

  fun cancelScheduledWork()

  suspend fun requestWidgetUpdate()

  fun canRequestRefreshImmediately(): Boolean = true
}
