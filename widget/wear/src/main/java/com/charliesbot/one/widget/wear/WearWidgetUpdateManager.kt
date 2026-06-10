package com.charliesbot.one.widget.wear

import android.content.Context
import androidx.glance.wear.GlanceWearWidgetManager
import com.charliesbot.shared.core.updates.DebouncedUpdateRequester
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal typealias WearWidgetUpdater = suspend (Context) -> Unit

class WearWidgetUpdateManager(
  private val applicationContext: Context,
  timeWindow: Long = 1000L,
  coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
  updateWidgets: WearWidgetUpdater = ::updateOneWearWidgets,
) {
  private val requester =
    DebouncedUpdateRequester(
      name = "WearWidgetUpdateManager",
      timeWindow = timeWindow,
      coroutineDispatcher = coroutineDispatcher,
      update = { updateWidgets(applicationContext) },
    )

  fun requestUpdate() = requester.requestUpdate()

  fun cancel() = requester.cancel()
}

private suspend fun updateOneWearWidgets(context: Context) {
  val widget = OneWearWidget()
  GlanceWearWidgetManager(context).fetchActiveWidgets(OneWearWidget::class).forEach {
    widget.triggerUpdate(context, it.instanceId)
  }
}
