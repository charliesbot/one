package com.charliesbot.one.di

import android.content.Context
import android.content.SharedPreferences
import com.charliesbot.one.data.AndroidStringProvider
import com.charliesbot.one.notifications.NotificationWorker
import com.charliesbot.shared.core.notifications.NotificationScheduler
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import com.charliesbot.one.today.TodayViewModel
import com.charliesbot.one.widgets.WidgetUpdateManager
import com.charliesbot.shared.core.abstraction.StringProvider
import org.koin.android.ext.koin.androidContext

val appModule = module {
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
        )
    }

    single<StringProvider> {
        AndroidStringProvider(androidContext())
    }

    viewModelOf(::TodayViewModel)
}