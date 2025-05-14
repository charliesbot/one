package com.charliesbot.one.services

import android.util.Log
import androidx.glance.appwidget.updateAll
import com.charliesbot.one.widgets.OneWidget
import com.charliesbot.shared.core.services.BaseFastingListenerService
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG

class FastingStateListenerService : BaseFastingListenerService() {
    override suspend fun onFastingStateSynced() {
        super.onFastingStateSynced()
        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName} - Overridden onFastingStateSynced: Triggering widget update."
        )

        OneWidget().updateAll(applicationContext)
    }

}
