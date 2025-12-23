package com.charliesbot.onewearos.presentation.services

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.charliesbot.onewearos.complication.ComplicationUpdateManager
import com.charliesbot.onewearos.presentation.notifications.OngoingActivityManager
import com.charliesbot.onewearos.tile.TileUpdateManager
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.services.FastingEventCallbacks

/**
 * Handles fasting events that originate locally ONLY on the watch (user actions).
 * Notifications are handled by [com.charliesbot.shared.core.services.FastingEventManager].
 */
class LocalWatchFastingCallbacks(
    private val context: Context,
    private val complicationUpdateManager: ComplicationUpdateManager,
    private val tileUpdateManager: TileUpdateManager,
    private val ongoingActivityManager: OngoingActivityManager
) : FastingEventCallbacks {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onFastingStarted(fastingDataItem: FastingDataItem) {
        Log.d(LOG_TAG, "LocalWatch: Processing LOCAL fasting start")
        val intent = OngoingActivityService.createStartIntent(
            context,
            fastingDataItem.startTimeInMillis,
            fastingDataItem.fastingGoalId
        )
        ContextCompat.startForegroundService(context, intent)
        complicationUpdateManager.requestUpdate()
        tileUpdateManager.requestUpdate()
        Log.d(LOG_TAG, "LocalWatch: Successfully handled local fasting start")
    }

    override suspend fun onFastingCompleted(fastingDataItem: FastingDataItem) {
        Log.d(LOG_TAG, "LocalWatch: Processing LOCAL fasting completion")
        val intent = OngoingActivityService.createStopIntent(context)
        context.startService(intent)  // Send stop action to the service
        complicationUpdateManager.requestUpdate()
        tileUpdateManager.requestUpdate()
        Log.d(LOG_TAG, "LocalWatch: Successfully handled local fasting completion")
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onFastingUpdated(fastingDataItem: FastingDataItem) {
        Log.d(LOG_TAG, "LocalWatch: Processing LOCAL fasting update")
        complicationUpdateManager.requestUpdate()
        tileUpdateManager.requestUpdate()
        ongoingActivityManager.requestUpdate()
        Log.d(LOG_TAG, "LocalWatch: Successfully handled local fasting update")
    }
}