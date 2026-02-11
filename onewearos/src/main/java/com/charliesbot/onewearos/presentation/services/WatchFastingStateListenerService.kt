package com.charliesbot.onewearos.presentation.services

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.charliesbot.onewearos.complication.ComplicationUpdateManager
import com.charliesbot.onewearos.tile.TileUpdateManager
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.DataLayerConstants
import com.charliesbot.shared.core.data.repositories.settingsRepository.SettingsRepository
import com.charliesbot.shared.core.data.repositories.settingsRepository.SmartReminderMode
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.notifications.NotificationScheduler
import com.charliesbot.shared.core.notifications.NotificationUtil
import com.charliesbot.shared.core.services.BaseFastingListenerService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.inject

class WatchFastingStateListenerService : BaseFastingListenerService() {
    private val complicationUpdateManager: ComplicationUpdateManager by inject()
    private val tileUpdateManager: TileUpdateManager by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val notificationScheduler: NotificationScheduler by inject()
    private val watchServiceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // This is called for general data syncs, not just start/stop
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun onPlatformFastingStateSynced() {
        super.onPlatformFastingStateSynced()
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Handling a remote data sync")
        complicationUpdateManager.requestUpdate()
        tileUpdateManager.requestUpdate()
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
        val intent = OngoingActivityService.createStartIntent(
            this,
            fastingDataItem.startTimeInMillis,
            fastingDataItem.fastingGoalId
        )
        ContextCompat.startForegroundService(this, intent)
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Fast started from REMOTE")
    }

    // Called when the PHONE stops a fast
    override suspend fun onPlatformFastingCompleted(fastingDataItem: FastingDataItem) {
        super.onPlatformFastingCompleted(fastingDataItem)
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Fast completed from REMOTE")
        val intent = OngoingActivityService.createStopIntent(this)
        startService(intent)  // Send stop action to the service
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // First, handle settings sync
        handleSettingsSync(dataEvents)
        // Handle smart reminder sync
        handleSmartReminderSync(dataEvents)
        // Then, call parent to handle fasting state
        super.onDataChanged(dataEvents)
    }

    private fun handleSmartReminderSync(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == DataLayerConstants.SMART_REMINDER_PATH
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val suggestedTimeMillis = dataMap.getLong(DataLayerConstants.SMART_REMINDER_SUGGESTED_TIME_KEY, 0L)
                val reasoning = dataMap.getString(DataLayerConstants.SMART_REMINDER_REASONING_KEY, "")

                Log.d(LOG_TAG, "WatchListener: Received smart reminder update - time: $suggestedTimeMillis, reason: $reasoning")

                if (suggestedTimeMillis > System.currentTimeMillis()) {
                    watchServiceScope.launch {
                        try {
                            // Ensure notification channel exists (safe to call multiple times)
                            // This guarantees notifications work even after app updates
                            NotificationUtil.createNotificationChannel(this@WatchFastingStateListenerService)
                            
                            // Schedule local notifications on the watch
                            notificationScheduler.scheduleSmartReminderNotifications(suggestedTimeMillis)
                            Log.d(LOG_TAG, "WatchListener: Smart reminder notifications scheduled on watch")
                        } catch (e: Exception) {
                            Log.e(LOG_TAG, "WatchListener: Failed to schedule smart reminder notifications", e)
                        }
                    }
                } else {
                    Log.d(LOG_TAG, "WatchListener: Smart reminder time has passed, skipping")
                }
            }
        }
    }

    private fun handleSettingsSync(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/settings") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val notificationsEnabled = dataMap.getBoolean("notifications_enabled", true)
                val notifyCompletion = dataMap.getBoolean("notify_completion", true)
                val notifyOneHourBefore = dataMap.getBoolean("notify_one_hour_before", true)
                val smartRemindersEnabled = dataMap.getBoolean("smart_reminders_enabled", false)
                val bedtimeMinutes = dataMap.getInt("bedtime_minutes", 1320)
                val fixedFastingStartMinutes = dataMap.getInt("fixed_fasting_start_minutes", 1140)
                val smartReminderModeString = dataMap.getString("smart_reminder_mode", SmartReminderMode.AUTO.name)
                val smartReminderMode = try {
                    SmartReminderMode.valueOf(smartReminderModeString)
                } catch (e: IllegalArgumentException) {
                    SmartReminderMode.AUTO
                }

                Log.d(
                    LOG_TAG,
                    "WatchListener: Received settings update from phone " +
                            "(notifications=$notificationsEnabled, completion=$notifyCompletion, oneHour=$notifyOneHourBefore, smartReminders=$smartRemindersEnabled, bedtime=$bedtimeMinutes, fixedStart=$fixedFastingStartMinutes, mode=$smartReminderMode)"
                )
                watchServiceScope.launch {
                    try {
                        // syncToRemote = false: Watch should NEVER sync settings back to phone
                        settingsRepository.setNotificationsEnabled(notificationsEnabled, syncToRemote = false)
                        settingsRepository.setNotifyOnCompletion(notifyCompletion, syncToRemote = false)
                        settingsRepository.setNotifyOneHourBefore(notifyOneHourBefore, syncToRemote = false)
                        settingsRepository.setSmartRemindersEnabled(smartRemindersEnabled, syncToRemote = false)
                        settingsRepository.setBedtimeMinutes(bedtimeMinutes, syncToRemote = false)
                        settingsRepository.setFixedFastingStartMinutes(fixedFastingStartMinutes, syncToRemote = false)
                        settingsRepository.setSmartReminderMode(smartReminderMode, syncToRemote = false)
                        Log.d(LOG_TAG, "WatchListener: Settings updated successfully (local only, no sync back)")
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "WatchListener: Failed to update settings", e)
                    }
                }
            }
        }
    }
}