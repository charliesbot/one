package com.charliesbot.onewearos.presentation.services

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.charliesbot.onewearos.complication.ComplicationUpdateManager
import com.charliesbot.onewearos.presentation.notifications.OngoingActivityManager
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.services.BaseFastingListenerService
import org.koin.core.component.inject

class WatchFastingStateListenerService : BaseFastingListenerService() {
    private val complicationUpdateManager: ComplicationUpdateManager by inject()
    private val ongoingActivityManager: OngoingActivityManager by inject()

    override suspend fun onPlatformFastingStateSynced() {
        super.onPlatformFastingStateSynced()
        val uniqueCallId = System.nanoTime()
        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName} - onFastingStateSynced on Watch: PRE-updateAll (Call ID: $uniqueCallId)"
        )
        complicationUpdateManager.requestUpdate()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onPlatformFastingStarted(fastingDataItem: FastingDataItem) {
        super.onPlatformFastingStarted(fastingDataItem)
        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName} - Fast started, creating ongoing activity"
        )
        ongoingActivityManager.startOngoingActivity(
            fastingDataItem.startTimeInMillis,
            fastingDataItem.fastingGoalId
        )
    }

    override suspend fun onPlatformFastingCompleted(fastingDataItem: FastingDataItem) {
        super.onPlatformFastingCompleted(fastingDataItem)
        Log.d(
            LOG_TAG,
            "${this::class.java.simpleName} - Fast completed, stopping ongoing activity"
        )
        ongoingActivityManager.stopOngoingActivity()
    }
}
