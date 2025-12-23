package com.charliesbot.one.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun updateWidgetPreview(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appwidgetManager = AppWidgetManager.getInstance(context)

                appwidgetManager.setWidgetPreview(
                    ComponentName(context, OneWidgetReceiver::class.java),
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                    OneWidgetPreview().compose(
                        context,
                        size = DpSize(200.dp, 200.dp),
                    ),
                )
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
            }
        }
    }
}

class OneWidgetPreview : GlanceAppWidget() {
    override val sizeMode: SizeMode
        get() = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                OneWidgetContent(
                    fastingData = widgetMockFastingData,
                    context = LocalContext.current
                )
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 200, heightDp = 200) // Max size
@Composable
private fun OneWidgetPreviewPreview() {
    OneWidgetContent(
        fastingData = widgetMockFastingData,
        context = LocalContext.current
    )
}
