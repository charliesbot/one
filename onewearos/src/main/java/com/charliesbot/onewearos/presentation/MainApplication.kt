package com.charliesbot.onewearos.presentation

import android.app.Application
import android.util.Log
import androidx.wear.phone.interactions.notifications.BridgingConfig
import androidx.wear.phone.interactions.notifications.BridgingManager
import androidx.work.Configuration
import com.charliesbot.onewearos.presentation.di.wearAppModule
import com.charliesbot.shared.core.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class MainApplication : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()

        disableNotificationBridge()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(sharedModule, wearAppModule)
        }
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

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

}