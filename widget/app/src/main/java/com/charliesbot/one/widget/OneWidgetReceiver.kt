package com.charliesbot.one.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.charliesbot.shared.core.domain.constants.AppConstants.LOG_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OneWidgetReceiver : GlanceAppWidgetReceiver(), KoinComponent {
  override val glanceAppWidget: GlanceAppWidget
    get() = OneWidget()

  private val scheduler: PhoneWidgetRefreshScheduler by inject()

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    Log.d(LOG_TAG, "OneWidgetReceiver: onEnabled - first widget placed")
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
      try {
        scheduler.enqueueImmediateRecovery()
      } finally {
        pendingResult.finish()
      }
    }
  }

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    Log.d(LOG_TAG, "OneWidgetReceiver: onDisabled - last widget removed")
    val pendingResult = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
      try {
        scheduler.cancel()
      } finally {
        pendingResult.finish()
      }
    }
  }

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray,
  ) {
    Log.d(
      LOG_TAG,
      "OneWidgetReceiver: onUpdate CALLED by system. AppWidgetIds: ${appWidgetIds.joinToString()}",
    )
    super.onUpdate(context, appWidgetManager, appWidgetIds)
  }
}
