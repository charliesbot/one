package com.charliesbot.one.widget.wear

import android.content.Context
import androidx.glance.wear.GlanceWearWidgetManager
import com.charliesbot.one.widget.common.WidgetHost

class WearWidgetHost(
  private val context: Context,
  private val activeWidgetChecker: suspend () -> Boolean = {
    GlanceWearWidgetManager(context).fetchActiveWidgets(OneWearWidget::class).isNotEmpty()
  },
  private val widgetUpdater: suspend (Context) -> Unit = { updateOneWearWidgets(it) },
) : WidgetHost {

  override suspend fun hasActiveWidgets(): Boolean = activeWidgetChecker()

  override suspend fun requestWidgetUpdate() {
    widgetUpdater(context)
  }

  override fun canEnqueueImmediateRecovery(): Boolean = true
}
