package com.charliesbot.one.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.charliesbot.shared.core.updates.DebouncedUpdateRequester
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class WidgetUpdateManager(
  private val applicationContext: Context,
  timeWindow: Long = 1000L,
  coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
  private val requester =
    DebouncedUpdateRequester(
      name = "WidgetUpdateManager",
      timeWindow = timeWindow,
      coroutineDispatcher = coroutineDispatcher,
      update = { OneWidget().updateAll(applicationContext) },
    )

  fun requestUpdate() = requester.requestUpdate()

  fun cancel() = requester.cancel()
}
