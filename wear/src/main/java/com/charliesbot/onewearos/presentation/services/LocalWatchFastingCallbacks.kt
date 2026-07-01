package com.charliesbot.onewearos.presentation.services

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.charliesbot.one.widget.wear.WearWidgetUpdateManager
import com.charliesbot.onewearos.complications.ComplicationUpdateManager
import com.charliesbot.onewearos.presentation.notifications.OngoingActivityManager
import com.charliesbot.shared.core.domain.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.domain.events.FastingEventCallbacks
import com.charliesbot.shared.core.models.FastingDataItem

/**
 * Handles fasting events that originate locally ONLY on the watch (user actions). Notifications are
 * handled by [com.charliesbot.shared.core.domain.events.FastingEventProcessor].
 */
class LocalWatchFastingCallbacks(
  private val complicationUpdateManager: ComplicationUpdateManager,
  private val ongoingActivityManager: OngoingActivityManager,
  private val wearWidgetUpdateManager: WearWidgetUpdateManager,
) : FastingEventCallbacks {
  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  override suspend fun onFastingStarted(fastingDataItem: FastingDataItem) {
    Log.d(LOG_TAG, "LocalWatch: Processing LOCAL fasting start")
    ongoingActivityManager.startOngoingActivity(
      fastingDataItem.startTimeInMillis,
      fastingDataItem.fastingGoalId,
    )
    complicationUpdateManager.requestUpdate()
    wearWidgetUpdateManager.requestUpdate()
    Log.d(LOG_TAG, "LocalWatch: Successfully handled local fasting start")
  }

  override suspend fun onFastingCompleted(fastingDataItem: FastingDataItem) {
    Log.d(LOG_TAG, "LocalWatch: Processing LOCAL fasting completion")
    ongoingActivityManager.stopOngoingActivity()
    complicationUpdateManager.requestUpdate()
    wearWidgetUpdateManager.requestUpdate()
    Log.d(LOG_TAG, "LocalWatch: Successfully handled local fasting completion")
  }

  @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
  override suspend fun onFastingUpdated(fastingDataItem: FastingDataItem) {
    Log.d(LOG_TAG, "LocalWatch: Processing LOCAL fasting update")
    complicationUpdateManager.requestUpdate()
    wearWidgetUpdateManager.requestUpdate()
    ongoingActivityManager.requestUpdate()
    Log.d(LOG_TAG, "LocalWatch: Successfully handled local fasting update")
  }
}
