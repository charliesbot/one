package com.charliesbot.onewearos.presentation.di

import androidx.room.Room
import com.charliesbot.onewearos.BuildConfig
import com.charliesbot.onewearos.complication.ComplicationUpdateManager
import com.charliesbot.onewearos.presentation.data.WearStringProvider
import com.charliesbot.onewearos.presentation.feature.settings.WearSettingsViewModel
import com.charliesbot.onewearos.presentation.notifications.NotificationWorker
import com.charliesbot.onewearos.presentation.notifications.OngoingActivityManager
import com.charliesbot.onewearos.presentation.services.LocalWatchFastingCallbacks
import com.charliesbot.onewearos.presentation.feature.today.WearTodayViewModel
import com.charliesbot.shared.core.abstraction.StringProvider
import com.charliesbot.shared.core.data.db.AppDatabase
import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepository
import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepositoryImpl
import com.charliesbot.shared.core.notifications.NotificationScheduler
import com.charliesbot.shared.core.services.FastingEventCallbacks
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val wearAppModule = module {
    // Room Database for Wear OS
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "fasting_history.db"
        ).apply {
            if (BuildConfig.DEBUG) {
                fallbackToDestructiveMigration(true)
            }
        }.build()
    }

    single {
        get<AppDatabase>().fastingRecordDao()
    }

    single<FastingHistoryRepository> {
        FastingHistoryRepositoryImpl(fastingRecordDao = get())
    }

    viewModelOf(::WearTodayViewModel)
    viewModelOf(::WearSettingsViewModel)

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
    single { LocalWatchFastingCallbacks(get(), get(), get()) }
    single<FastingEventCallbacks> { get<LocalWatchFastingCallbacks>() }
}