package com.charliesbot.one.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.updateAll
import com.charliesbot.one.widget.common.WidgetHost

class PhoneWidgetHost(
  private val context: Context,
  private val activeWidgetChecker: () -> Boolean = {
    AppWidgetManager.getInstance(context)
      .getAppWidgetIds(ComponentName(context, OneWidgetReceiver::class.java))
      .isNotEmpty()
  },
  private val widgetUpdater: suspend (Context) -> Unit = { OneWidget().updateAll(it) },
) : WidgetHost {

  override suspend fun hasActiveWidgets(): Boolean = activeWidgetChecker()

  override suspend fun requestWidgetUpdate() {
    widgetUpdater(context)
  }

  override fun canRequestRefreshImmediately(): Boolean = activeWidgetChecker()
}
