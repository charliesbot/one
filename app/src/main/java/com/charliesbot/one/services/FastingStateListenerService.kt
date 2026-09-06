package com.charliesbot.one.services

import android.util.Log
import com.charliesbot.one.widget.PhoneWidgetRefreshScheduler
import com.charliesbot.one.widget.WidgetUpdateManager
import com.charliesbot.shared.core.data.services.BaseFastingListenerService
import com.charliesbot.shared.core.domain.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.domain.repository.FastingHistoryRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.models.FastingRecord
import org.koin.core.component.inject

class FastingStateListenerService : BaseFastingListenerService() {
  private val widgetUpdateManager: WidgetUpdateManager by inject()
  private val fastingHistoryRepository: FastingHistoryRepository by inject()
  private val phoneWidgetRefreshScheduler: PhoneWidgetRefreshScheduler by inject()

  override suspend fun onPlatformFastingStarted(fastingDataItem: FastingDataItem) {
    super.onPlatformFastingStarted(fastingDataItem)
    phoneWidgetRefreshScheduler.onFastingStartedOrUpdated(fastingDataItem)
  }

  override suspend fun onPlatformFastingUpdated(fastingDataItem: FastingDataItem) {
    super.onPlatformFastingUpdated(fastingDataItem)
    phoneWidgetRefreshScheduler.onFastingStartedOrUpdated(fastingDataItem)
  }

  // Called when the WATCH stops a fast
  override suspend fun onPlatformFastingCompleted(fastingDataItem: FastingDataItem) {
    super.onPlatformFastingCompleted(fastingDataItem)
    phoneWidgetRefreshScheduler.onFastingCompleted()
    fastingHistoryRepository.saveFastingRecord(
      FastingRecord(
        startTimeEpochMillis = fastingDataItem.startTimeInMillis,
        endTimeEpochMillis = fastingDataItem.updateTimestamp,
        fastingGoalId = fastingDataItem.fastingGoalId,
      )
    )
  }

  override suspend fun onPlatformFastingStateSynced() {
    super.onPlatformFastingStateSynced()
    val uniqueCallId = System.nanoTime()
    Log.d(
      LOG_TAG,
      "${this::class.java.simpleName} - onFastingStateSynced: PRE-updateAll (Call ID: $uniqueCallId)",
    )
    widgetUpdateManager.requestUpdate()
  }
}
