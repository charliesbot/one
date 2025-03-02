package com.charliesbot.onewearos.presentation

import android.app.Application
import com.charliesbot.onewearos.presentation.di.wearAppModule
import com.charliesbot.shared.core.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin


class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(sharedModule, wearAppModule)
        }
    }
}