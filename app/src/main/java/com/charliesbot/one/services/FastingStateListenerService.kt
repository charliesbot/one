package com.charliesbot.one.services

import android.util.Log
import com.charliesbot.one.widget.WidgetUpdateManager
import com.charliesbot.one.widget.common.WidgetRefreshScheduler
import com.charliesbot.shared.core.data.services.BaseFastingListenerService
import com.charliesbot.shared.core.domain.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.domain.repository.FastingHistoryRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.models.FastingRecord
import org.koin.core.component.inject

class FastingStateListenerService : BaseFastingListenerService() {
  private val widgetUpdateManager: WidgetUpdateManager by inject()
  private val fastingHistoryRepository: FastingHistoryRepository by inject()
  private val phoneWidgetRefreshScheduler: WidgetRefreshScheduler by inject()

  // Called when the WATCH starts a fast
  override suspend fun onPlatformFastingStarted(fastingDataItem: FastingDataItem) {
    super.onPlatformFastingStarted(fastingDataItem)
    phoneWidgetRefreshScheduler.reconcile()
  }

  override suspend fun onPlatformFastingUpdated(fastingDataItem: FastingDataItem) {
    super.onPlatformFastingUpdated(fastingDataItem)
    phoneWidgetRefreshScheduler.reconcile()
  }

  // Called when the WATCH stops a fast
  override suspend fun onPlatformFastingCompleted(fastingDataItem: FastingDataItem) {
    super.onPlatformFastingCompleted(fastingDataItem)
    phoneWidgetRefreshScheduler.reconcile()
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

  // Note: Settings sync is ONE-WAY (phone → watch only)
  // The phone does NOT listen to settings from the watch
  // Only fasting state is bidirectional
}
