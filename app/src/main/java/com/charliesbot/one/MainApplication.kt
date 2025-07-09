package com.charliesbot.one

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.os.Build
import android.util.Log
import androidx.glance.appwidget.compose
import com.charliesbot.one.di.appModule
import com.charliesbot.one.widgets.OneWidgetReceiver
import com.charliesbot.shared.core.di.sharedModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import com.charliesbot.shared.core.constants.AppConstants

class MainApplication : Application() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Log Koin into Android logger
            androidLogger()
            // Reference Android context
            androidContext(this@MainApplication)
            // Load modules
            modules(
                sharedModule,
                appModule
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            coroutineScope.launch {
                try {
                    Log.d(
                        AppConstants.LOG_TAG,
                        "Widget Preview - Attempting to generate and set widget preview."
                    )
                    val provider =
                        ComponentName(this@MainApplication, OneWidgetReceiver::class.java)
                    val widgetPreview = OneWidgetReceiver().preview.compose(
                        context = this@MainApplication
                    )
                    AppWidgetManager.getInstance(this@MainApplication).setWidgetPreview(
                        provider,
                        AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                        widgetPreview
                    )
                    Log.d(AppConstants.LOG_TAG, "Widget Preview - Successfully set widget preview.")
                } catch (e: Exception) {
                    // This will catch any errors during compose() or setWidgetPreview()
                    Log.e(AppConstants.LOG_TAG, "Widget Preview - Failed to set widget preview.", e)
                }
            }
        }
    }
}