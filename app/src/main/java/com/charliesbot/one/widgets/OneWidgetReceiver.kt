package com.charliesbot.one.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG

class OneWidgetReceiver : GlanceAppWidgetReceiver() {
    val preview: GlanceAppWidget = OneWidget(isPreview = true)
    override val glanceAppWidget: GlanceAppWidget
        get() = OneWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(
            LOG_TAG,
            "OneWidgetReceiver: onUpdate CALLED by system. AppWidgetIds: ${appWidgetIds.joinToString()}"
        )
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}
