package com.charliesbot.one.features.dashboard.di

import com.charliesbot.one.features.dashboard.TodayViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val dashboardModule = module { viewModelOf(::TodayViewModel) }
