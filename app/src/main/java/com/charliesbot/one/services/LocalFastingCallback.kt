package com.charliesbot.one.services

import android.util.Log
import com.charliesbot.one.widget.PhoneWidgetRefreshScheduler
import com.charliesbot.one.widget.WidgetUpdateManager
import com.charliesbot.shared.core.domain.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.domain.events.FastingEventCallbacks
import com.charliesbot.shared.core.domain.repository.FastingHistoryRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.models.FastingRecord
import com.charliesbot.shared.core.utils.GoalResolver

class LocalFastingCallback(
  private val widgetUpdateManager: WidgetUpdateManager,
  private val fastingHistoryRepository: FastingHistoryRepository,
  private val phoneWidgetRefreshScheduler: PhoneWidgetRefreshScheduler,
  private val goalResolver: GoalResolver,
) : FastingEventCallbacks {
  override suspend fun onFastingStarted(fastingDataItem: FastingDataItem) {
    Log.d(LOG_TAG, "LocalFastingCallback: Processing LOCAL fasting start")
    widgetUpdateManager.requestUpdate()
    val goalDuration = goalResolver.resolveGoalDurationMillis(fastingDataItem.fastingGoalId)
    phoneWidgetRefreshScheduler.scheduleNext(
      startTimeMillis = fastingDataItem.startTimeInMillis,
      goalDurationMillis = goalDuration,
    )
  }

  override suspend fun onFastingUpdated(fastingDataItem: FastingDataItem) {
    Log.d(LOG_TAG, "LocalFastingCallback: Processing LOCAL fasting update")
    widgetUpdateManager.requestUpdate()
    val goalDuration = goalResolver.resolveGoalDurationMillis(fastingDataItem.fastingGoalId)
    phoneWidgetRefreshScheduler.scheduleNext(
      startTimeMillis = fastingDataItem.startTimeInMillis,
      goalDurationMillis = goalDuration,
    )
  }

  override suspend fun onFastingCompleted(fastingDataItem: FastingDataItem) {
    Log.d(LOG_TAG, "LocalFastingCallback: Processing LOCAL fasting completion")
    phoneWidgetRefreshScheduler.cancel()
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
