package com.charliesbot.onewearos.presentation.services

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.charliesbot.onewearos.complication.ComplicationUpdateManager
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.constants.DataLayerConstants
import com.charliesbot.shared.core.data.repositories.customGoalRepository.CustomGoalRepository
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
    private val settingsRepository: SettingsRepository by inject()
    private val customGoalRepository: CustomGoalRepository by inject()
    private val notificationScheduler: NotificationScheduler by inject()
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
        val intent = OngoingActivityService.createStartIntent(
            this,
            fastingDataItem.startTimeInMillis,
            fastingDataItem.fastingGoalId,
        )
        try {
            ContextCompat.startForegroundService(this, intent)
        } catch (e: ForegroundServiceStartNotAllowedException) {
            Log.w(LOG_TAG, "${this::class.java.simpleName} - Cannot start ongoing activity — app is in background", e)
        }
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Fast started from REMOTE")
    }

    // Called when the PHONE stops a fast
    override suspend fun onPlatformFastingCompleted(fastingDataItem: FastingDataItem) {
        super.onPlatformFastingCompleted(fastingDataItem)
        Log.d(LOG_TAG, "${this::class.java.simpleName} - Fast completed from REMOTE")
        val intent = OngoingActivityService.createStopIntent(this)
        startService(intent) // Send stop action to the service
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // First, handle settings sync and capture the smart reminders setting if present
        val smartRemindersEnabled = handleSettingsSync(dataEvents)
        // Handle smart reminder sync, threading the settings value to avoid race condition
        handleSmartReminderSync(dataEvents, smartRemindersEnabled)
        // Handle custom goals sync
        handleCustomGoalsSync(dataEvents)
        // Then, call parent to handle fasting state
        super.onDataChanged(dataEvents)
    }

    /**
     * @param smartRemindersEnabledOverride When non-null, comes from the same onDataChanged batch
     *   (settings were synced alongside the reminder). This avoids a race condition where the
     *   DataStore hasn't persisted the setting yet when we try to read it.
     *   - true → schedule using forced variant (skip DataStore check)
     *   - false → skip scheduling (reminders were just disabled)
     *   - null → no settings in this batch, fall back to reading DataStore
     */
    private fun handleSmartReminderSync(dataEvents: DataEventBuffer, smartRemindersEnabledOverride: Boolean?) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == DataLayerConstants.SMART_REMINDER_PATH
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val suggestedTimeMillis = dataMap.getLong(DataLayerConstants.SMART_REMINDER_SUGGESTED_TIME_KEY, 0L)
                val reasoning = dataMap.getString(DataLayerConstants.SMART_REMINDER_REASONING_KEY, "")

                Log.d(
                    LOG_TAG,
                    "WatchListener: Received smart reminder update - time: $suggestedTimeMillis, reason: $reasoning",
                )

                if (suggestedTimeMillis > System.currentTimeMillis()) {
                    when (smartRemindersEnabledOverride) {
                        false -> {
                            Log.d(LOG_TAG, "WatchListener: Smart reminders disabled in this batch, skipping")
                        }

                        true -> {
                            // Settings arrived in the same batch — use forced variant to skip DataStore read
                            NotificationUtil.createNotificationChannel(this@WatchFastingStateListenerService)
                            notificationScheduler.scheduleSmartReminderNotificationsForced(suggestedTimeMillis)
                            Log.d(LOG_TAG, "WatchListener: Smart reminder notifications scheduled on watch (forced)")
                        }

                        null -> {
                            // No settings in this batch — safe to read DataStore (it's already persisted)
                            watchServiceScope.launch {
                                try {
                                    NotificationUtil.createNotificationChannel(this@WatchFastingStateListenerService)
                                    notificationScheduler.scheduleSmartReminderNotifications(suggestedTimeMillis)
                                    Log.d(LOG_TAG, "WatchListener: Smart reminder notifications scheduled on watch")
                                } catch (e: Exception) {
                                    Log.e(LOG_TAG, "WatchListener: Failed to schedule smart reminder notifications", e)
                                }
                            }
                        }
                    }
                } else {
                    Log.d(LOG_TAG, "WatchListener: Smart reminder time has passed, skipping")
                }
            }
        }
    }

    private fun handleCustomGoalsSync(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == DataLayerConstants.CUSTOM_GOALS_PATH
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val goalsJson = dataMap.getString(DataLayerConstants.CUSTOM_GOALS_JSON_KEY, "[]")

                Log.d(LOG_TAG, "WatchListener: Received custom goals update from phone")

                watchServiceScope.launch {
                    try {
                        customGoalRepository.replaceAllCustomGoalsFromJson(goalsJson, syncToRemote = false)
                        Log.d(LOG_TAG, "WatchListener: Custom goals updated successfully")
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "WatchListener: Failed to update custom goals", e)
                    }
                }
            }
        }
    }

    /**
     * @return The value of `smart_reminders_enabled` parsed from the batch, or `null` if no
     *   `/settings` event was present. This is threaded to [handleSmartReminderSync] so it can
     *   decide whether to use the forced scheduling path.
     */
    private fun handleSettingsSync(dataEvents: DataEventBuffer): Boolean? {
        var smartRemindersEnabledResult: Boolean? = null

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

                smartRemindersEnabledResult = smartRemindersEnabled

                Log.d(
                    LOG_TAG,
                    "WatchListener: Received settings update from phone " +
                        "(notifications=$notificationsEnabled, completion=$notifyCompletion, oneHour=$notifyOneHourBefore, smartReminders=$smartRemindersEnabled, bedtime=$bedtimeMinutes, fixedStart=$fixedFastingStartMinutes, mode=$smartReminderMode)",
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

        return smartRemindersEnabledResult
    }
}
