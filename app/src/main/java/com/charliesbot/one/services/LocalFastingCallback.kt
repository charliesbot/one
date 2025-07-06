package com.charliesbot.one.services

import android.util.Log
import com.charliesbot.one.widgets.WidgetUpdateManager
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.data.db.FastingRecord
import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.services.FastingEventCallbacks

class LocalFastingCallback(
    private val widgetUpdateManager: WidgetUpdateManager,
    private val fastingHistoryRepository: FastingHistoryRepository,
) : FastingEventCallbacks {
    override suspend fun onFastingStarted(fastingDataItem: FastingDataItem) {
        Log.d(LOG_TAG, "LocalFastingCallback: Processing LOCAL fasting start")
        widgetUpdateManager.requestUpdate()
    }

    override suspend fun onFastingUpdated(fastingDataItem: FastingDataItem) {
        Log.d(LOG_TAG, "LocalFastingCallback: Processing LOCAL fasting update")
        widgetUpdateManager.requestUpdate()
    }

    override suspend fun onFastingCompleted(fastingDataItem: FastingDataItem) {
        Log.d(LOG_TAG, "LocalFastingCallback: Processing LOCAL fasting completion")
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