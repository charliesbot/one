package com.charliesbot.onewearos.presentation

import android.app.Application
import android.util.Log
import androidx.wear.phone.interactions.notifications.BridgingConfig
import androidx.wear.phone.interactions.notifications.BridgingManager
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.charliesbot.onewearos.presentation.di.wearAppModule
import com.charliesbot.onewearos.presentation.workers.DailyNotificationSchedulerWorker
import com.charliesbot.shared.core.data.repositories.preferencesRepository.PreferencesRepository
import com.charliesbot.shared.core.di.sharedModule
import com.charliesbot.shared.core.notifications.NotificationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class MainApplication : Application(), Configuration.Provider {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()

        disableNotificationBridge()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(sharedModule, wearAppModule)
        }

        // Create notification channel for wear notifications
        NotificationUtil.createNotificationChannel(this)

        // Schedule daily smart notification worker
        initializeDailyNotificationWorker()
    }

    private fun disableNotificationBridge() {
        try {
            val config =
                BridgingConfig.Builder(this, false)
                    .build()
            BridgingManager.fromContext(this).setConfig(config)
            Log.d("WatchApplication", "Notification bridging disabled successfully.")
        } catch (e: Exception) {
            Log.e("WatchApplication", "Failed to disable notification bridging", e)
        }
    }

    private fun initializeDailyNotificationWorker() {
        val preferencesRepository: PreferencesRepository by inject()
        
        // Use application scope to check preferences asynchronously
        applicationScope.launch {
            try {
                val smartNotificationsEnabled = preferencesRepository.getSmartNotificationsEnabled().first()
                
                if (smartNotificationsEnabled) {
                    // Calculate delay until next midnight
                    val now = LocalDateTime.now()
                    val nextMidnight = now.plusDays(1).truncatedTo(ChronoUnit.DAYS)
                    val delayUntilMidnight = ChronoUnit.MILLIS.between(now, nextMidnight)
                    
                    // Create periodic work request that runs daily
                    val workRequest = PeriodicWorkRequestBuilder<DailyNotificationSchedulerWorker>(
                        24, TimeUnit.HOURS
                    )
                        .setInitialDelay(delayUntilMidnight, TimeUnit.MILLISECONDS)
                        .build()
                    
                    WorkManager.getInstance(this@MainApplication)
                        .enqueueUniquePeriodicWork(
                            "daily-smart-notification-scheduler",
                            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
                            workRequest
                        )
                    
                    Log.d("WatchApplication", "Daily notification worker scheduled successfully")
                }
            } catch (e: Exception) {
                Log.e("WatchApplication", "Failed to initialize daily notification worker", e)
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

}