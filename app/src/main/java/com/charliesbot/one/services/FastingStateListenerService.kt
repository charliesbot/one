package com.charliesbot.one.services

import android.util.Log
import com.charliesbot.one.widgets.WidgetUpdateManager
import com.charliesbot.shared.core.services.BaseFastingListenerService
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.data.db.FastingRecord
import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepository
import com.charliesbot.shared.core.data.repositories.settingsRepository.SettingsRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class FastingStateListenerService : BaseFastingListenerService() {
    private val widgetUpdateManager: WidgetUpdateManager by inject()
    private val fastingHistoryRepository: FastingHistoryRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val phoneServiceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Called when the WATCH starts a fast
    override suspend fun onPlatformFastingCompleted(fastingDataItem: FastingDataItem) {
        super.onPlatformFastingCompleted(fastingDataItem)
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
            "${this::class.java.simpleName} - onFastingStateSynced: PRE-updateAll (Call ID: $uniqueCallId)"
        )
        widgetUpdateManager.requestUpdate()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // First, handle settings sync
        handleSettingsSync(dataEvents)
        // Then, call parent to handle fasting state
        super.onDataChanged(dataEvents)
    }

    private fun handleSettingsSync(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/settings") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val notificationsEnabled = dataMap.getBoolean("notifications_enabled", true)
                val notifyCompletion = dataMap.getBoolean("notify_completion", true)
                val notifyOneHourBefore = dataMap.getBoolean("notify_one_hour_before", true)

                Log.d(LOG_TAG, "PhoneListener: Received settings update from watch")
                phoneServiceScope.launch {
                    try {
                        settingsRepository.setNotificationsEnabled(notificationsEnabled)
                        settingsRepository.setNotifyOnCompletion(notifyCompletion)
                        settingsRepository.setNotifyOneHourBefore(notifyOneHourBefore)
                        Log.d(LOG_TAG, "PhoneListener: Settings updated successfully")
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "PhoneListener: Failed to update settings", e)
                    }
                }
            }
        }
    }
}
