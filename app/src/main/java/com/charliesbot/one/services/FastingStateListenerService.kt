package com.charliesbot.one.services

import android.util.Log
import androidx.glance.appwidget.updateAll
import com.charliesbot.one.widgets.OneWidget
import com.charliesbot.one.widgets.WidgetUpdateManager
import com.charliesbot.shared.core.services.BaseFastingListenerService
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import org.koin.core.component.inject

class FastingStateListenerService : BaseFastingListenerService() {
    private val widgetUpdateManager: WidgetUpdateManager by inject()

    override suspend fun onFastingStateSynced() {
        super.onFastingStateSynced()
        val uniqueCallId = System.nanoTime()
        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName} - onFastingStateSynced: PRE-updateAll (Call ID: $uniqueCallId)"
        )
        widgetUpdateManager.requestUpdate()
    }
}
