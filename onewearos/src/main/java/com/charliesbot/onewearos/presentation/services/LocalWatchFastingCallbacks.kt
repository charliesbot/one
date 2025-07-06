package com.charliesbot.onewearos.presentation.services

import android.Manifest
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
    private val complicationUpdateManager: ComplicationUpdateManager,
    private val ongoingActivityManager: OngoingActivityManager,
) : FastingEventCallbacks {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onFastingStarted(fastingDataItem: FastingDataItem) {
        Log.d(LOG_TAG, "LocalWatch: Processing LOCAL fasting start")

        try {
            ongoingActivityManager.startOngoingActivity(
                fastingDataItem.startTimeInMillis,
                fastingDataItem.fastingGoalId
            )
            complicationUpdateManager.requestUpdate()
            Log.d(LOG_TAG, "LocalWatch: Successfully handled local fasting start")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "LocalWatch: Failed to handle local fasting start", e)
            throw e
        }
    }

    override suspend fun onFastingCompleted(fastingDataItem: FastingDataItem) {
        Log.d(LOG_TAG, "LocalWatch: Processing LOCAL fasting completion")

        try {
            ongoingActivityManager.stopOngoingActivity()
            complicationUpdateManager.requestUpdate()
            Log.d(LOG_TAG, "LocalWatch: Successfully handled local fasting completion")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "LocalWatch: Failed to handle local fasting completion", e)
            throw e
        }
    }

    override suspend fun onFastingUpdated(fastingDataItem: FastingDataItem) {
        Log.d(LOG_TAG, "LocalWatch: Processing LOCAL fasting update")
    }
}