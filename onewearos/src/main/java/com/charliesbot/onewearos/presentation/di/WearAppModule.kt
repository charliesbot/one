package com.charliesbot.onewearos.presentation.di

import com.charliesbot.onewearos.presentation.today.WearTodayViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val wearAppModule = module {
    viewModelOf(::WearTodayViewModel)
}