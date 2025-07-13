package com.charliesbot.onewearos.presentation.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import com.charliesbot.onewearos.complication.ComplicationUpdateManager
import com.charliesbot.onewearos.presentation.notifications.OngoingActivityManager
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.services.FastingEventCallbacks

/**
 * Handles fasting events that originate locally ONLY on the watch (user actions).
 * Notifications are handled by [com.charliesbot.shared.core.services.FastingEventManager].
 */
class LocalWatchFastingCallbacks(
    private val context: Context,
    private val complicationUpdateManager: ComplicationUpdateManager
) : FastingEventCallbacks {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onFastingStarted(fastingDataItem: FastingDataItem) {
        Log.d(LOG_TAG, "LocalWatch: Processing LOCAL fasting start")
        val intent = Intent(context, OngoingActivityService::class.java)
        context.startForegroundService(intent)
        complicationUpdateManager.requestUpdate()
        Log.d(LOG_TAG, "LocalWatch: Successfully handled local fasting start")
    }

    override suspend fun onFastingCompleted(fastingDataItem: FastingDataItem) {
        Log.d(LOG_TAG, "LocalWatch: Processing LOCAL fasting completion")
        val intent = Intent(context, OngoingActivityService::class.java)
        context.stopService(intent)
        complicationUpdateManager.requestUpdate()
        Log.d(LOG_TAG, "LocalWatch: Successfully handled local fasting completion")
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onFastingUpdated(fastingDataItem: FastingDataItem) {
        Log.d(LOG_TAG, "LocalWatch: Processing LOCAL fasting update")
    }
}