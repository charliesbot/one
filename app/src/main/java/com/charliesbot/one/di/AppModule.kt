package com.charliesbot.one.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.charliesbot.one.BuildConfig
import com.charliesbot.one.data.AndroidStringProvider
import com.charliesbot.one.data.FastingHistoryRepositoryImpl
import com.charliesbot.one.notifications.NotificationWorker
import com.charliesbot.one.services.LocalFastingCallback
import com.charliesbot.one.services.SmartReminderCallbackImpl
import com.charliesbot.shared.core.notifications.NotificationScheduler
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import com.charliesbot.one.features.dashboard.TodayViewModel
import com.charliesbot.one.features.profile.YouViewModel
import com.charliesbot.one.features.settings.SettingsViewModel
import com.charliesbot.one.widgets.WidgetUpdateManager
import com.charliesbot.shared.core.abstraction.StringProvider
import com.charliesbot.shared.core.data.db.AppDatabase
import com.charliesbot.shared.core.data.repositories.fastingHistoryRepository.FastingHistoryRepository
import com.charliesbot.shared.core.domain.usecase.GetMonthlyFastingMapUseCase
import com.charliesbot.shared.core.services.FastingEventCallbacks
import com.charliesbot.shared.core.services.SmartReminderCallback
import org.koin.android.ext.koin.androidContext

val appModule = module {
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

    single<SharedPreferences> {
        androidContext().getSharedPreferences(
            "one_fasting_prefs",
            Context.MODE_PRIVATE
        )
    }

    single<WidgetUpdateManager> {
        WidgetUpdateManager(androidContext())
    }

    single<NotificationScheduler> {
        NotificationScheduler(
            context = androidContext(),
            workerClass = NotificationWorker::class.java,
            settingsRepository = get(),
        )
    }

    single<FastingHistoryRepository> {
        FastingHistoryRepositoryImpl(fastingRecordDao = get())
    }

    single<StringProvider> {
        AndroidStringProvider(androidContext())
    }

    factory{ GetMonthlyFastingMapUseCase(get()) }

    viewModelOf(::TodayViewModel)
    viewModelOf(::YouViewModel)
    viewModelOf(::SettingsViewModel)

    single { LocalFastingCallback(get(), get()) }
    single<FastingEventCallbacks> { get<LocalFastingCallback>() }

    single<SmartReminderCallback> { SmartReminderCallbackImpl(androidContext()) }
}