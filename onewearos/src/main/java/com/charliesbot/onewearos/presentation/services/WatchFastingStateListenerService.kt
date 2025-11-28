package com.charliesbot.onewearos.presentation.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.charliesbot.onewearos.R
import com.charliesbot.onewearos.complication.ComplicationUpdateManager
import com.charliesbot.onewearos.presentation.notifications.OngoingActivityManager
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.data.repositories.settingsRepository.SettingsRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.services.BaseFastingListenerService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class WatchFastingStateListenerService : BaseFastingListenerService() {
    private val complicationUpdateManager: ComplicationUpdateManager by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val watchServiceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // This is called for general data syncs, not just start/stop
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onPlatformFastingStateSynced() {
        super.onPlatformFastingStateSynced()
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Handling a remote data sync")
        complicationUpdateManager.requestUpdate()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Creating service")
    }

    override fun onDestroy() {
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Service being destroyed")
        super.onDestroy()
    }

    // Called when the PHONE starts a fast
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onPlatformFastingStarted(fastingDataItem: FastingDataItem) {
        super.onPlatformFastingStarted(fastingDataItem)
        val intent = Intent(this, OngoingActivityService::class.java)
        startForegroundService(intent)
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Fast started from REMOTE")
    }

    // Called when the PHONE stops a fast
    override suspend fun onPlatformFastingCompleted(fastingDataItem: FastingDataItem) {
        super.onPlatformFastingCompleted(fastingDataItem)
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Fast completed from REMOTE")
        val intent = Intent(this, OngoingActivityService::class.java)
        stopService(intent)
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

                Log.d(LOG_TAG, "WatchListener: Received settings update from phone")
                watchServiceScope.launch {
                    try {
                        // syncToRemote = false: Watch should NEVER sync settings back to phone
                        settingsRepository.setNotificationsEnabled(notificationsEnabled, syncToRemote = false)
                        settingsRepository.setNotifyOnCompletion(notifyCompletion, syncToRemote = false)
                        settingsRepository.setNotifyOneHourBefore(notifyOneHourBefore, syncToRemote = false)
                        Log.d(LOG_TAG, "WatchListener: Settings updated successfully (local only, no sync back)")
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "WatchListener: Failed to update settings", e)
                    }
                }
            }
        }
    }
}