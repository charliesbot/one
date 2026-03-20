package com.charliesbot.onewearos.di

import com.charliesbot.onewearos.WearTodayViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val wearDashboardModule = module { viewModelOf(::WearTodayViewModel) }
