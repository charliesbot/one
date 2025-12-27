package com.charliesbot.one

import android.app.Application
import com.charliesbot.one.di.appModule
import com.charliesbot.one.notifications.SmartReminderWorker
import com.charliesbot.shared.core.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Log Koin into Android logger
            androidLogger()
            // Reference Android context
            androidContext(this@MainApplication)
            // Load modules
            modules(
                sharedModule,
                appModule
            )
        }

        // Schedule the daily smart reminder worker
        // The worker itself checks if smart reminders are enabled before executing
        SmartReminderWorker.scheduleDailyWorker(this)
    }
}