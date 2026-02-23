package com.charliesbot.onewearos.di

import com.charliesbot.onewearos.presentation.feature.today.WearTodayViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val wearDashboardModule = module {
    viewModelOf(::WearTodayViewModel)
}
