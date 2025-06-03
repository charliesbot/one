package com.charliesbot.onewearos.presentation.services

import android.util.Log
import com.charliesbot.onewearos.complication.ComplicationUpdateManager
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.services.BaseFastingListenerService
import org.koin.core.component.inject

class WatchFastingStateListenerService : BaseFastingListenerService() {
    private val complicationUpdateManager: ComplicationUpdateManager by inject()

    override suspend fun onFastingStateSynced() {
        super.onFastingStateSynced()
        val uniqueCallId = System.nanoTime()
        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName} - onFastingStateSynced on Watch: PRE-updateAll (Call ID: $uniqueCallId)"
        )
        complicationUpdateManager.requestUpdate()
    }
}
