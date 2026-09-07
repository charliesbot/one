package com.charliesbot.one.services

import android.util.Log
import com.charliesbot.one.widget.WidgetUpdateManager
import com.charliesbot.one.widget.common.WidgetRefreshScheduler
import com.charliesbot.shared.core.domain.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.domain.events.FastingEventCallbacks
import com.charliesbot.shared.core.domain.repository.FastingHistoryRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.models.FastingRecord

class LocalFastingCallback(
  private val widgetUpdateManager: WidgetUpdateManager,
  private val fastingHistoryRepository: FastingHistoryRepository,
  private val phoneWidgetRefreshScheduler: WidgetRefreshScheduler,
) : FastingEventCallbacks {
  override suspend fun onFastingStarted(fastingDataItem: FastingDataItem) {
    Log.d(LOG_TAG, "LocalFastingCallback: Processing LOCAL fasting start")
    widgetUpdateManager.requestUpdate()
    phoneWidgetRefreshScheduler.onFastingStartedOrUpdated(fastingDataItem)
  }

  override suspend fun onFastingUpdated(fastingDataItem: FastingDataItem) {
    Log.d(LOG_TAG, "LocalFastingCallback: Processing LOCAL fasting update")
    widgetUpdateManager.requestUpdate()
    phoneWidgetRefreshScheduler.onFastingStartedOrUpdated(fastingDataItem)
  }

  override suspend fun onFastingCompleted(fastingDataItem: FastingDataItem) {
    Log.d(LOG_TAG, "LocalFastingCallback: Processing LOCAL fasting completion")
    phoneWidgetRefreshScheduler.onFastingCompleted()
    widgetUpdateManager.requestUpdate()
    fastingHistoryRepository.saveFastingRecord(
      FastingRecord(
        startTimeEpochMillis = fastingDataItem.startTimeInMillis,
        endTimeEpochMillis = fastingDataItem.updateTimestamp,
        fastingGoalId = fastingDataItem.fastingGoalId,
      )
    )
  }
}
