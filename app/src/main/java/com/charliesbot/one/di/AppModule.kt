package com.charliesbot.one.di

import android.content.Context
import android.content.SharedPreferences
import com.charliesbot.one.notifications.NotificationScheduler
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import com.charliesbot.one.today.TodayViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf

val appModule = module {
    single<SharedPreferences> {
        androidContext().getSharedPreferences(
            "one_fasting_prefs",
            Context.MODE_PRIVATE
        )
    }
    singleOf(::NotificationScheduler)
    viewModelOf(::TodayViewModel)
}