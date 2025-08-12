package com.charliesbot.onewearos.presentation.di

import com.charliesbot.onewearos.complication.ComplicationUpdateManager
import com.charliesbot.onewearos.presentation.data.WearStringProvider
import com.charliesbot.onewearos.presentation.notifications.NotificationWorker
import com.charliesbot.onewearos.presentation.notifications.OngoingActivityManager
import com.charliesbot.onewearos.presentation.services.LocalWatchFastingCallbacks
import com.charliesbot.onewearos.presentation.feature.today.WearTodayViewModel
import com.charliesbot.shared.core.abstraction.StringProvider
import com.charliesbot.shared.core.notifications.NotificationScheduler
import com.charliesbot.shared.core.services.FastingEventCallbacks
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val wearAppModule = module {
    viewModelOf(::WearTodayViewModel)
    single<NotificationScheduler> {
        NotificationScheduler(
            context = androidContext(),
            workerClass = NotificationWorker::class.java,
        )
    }
    single<StringProvider> {
        WearStringProvider(androidContext())
    }
    single<ComplicationUpdateManager> {
        ComplicationUpdateManager(androidContext())
    }
    single<OngoingActivityManager> {
        OngoingActivityManager(
            context = androidContext(),
            fastingDataRepository = get()
        )
    }
    single { LocalWatchFastingCallbacks(get(), get()) }
    single<FastingEventCallbacks> { get<LocalWatchFastingCallbacks>() }
}