package com.charliesbot.onewearos.presentation.di

import com.charliesbot.onewearos.presentation.today.WearTodayViewModel
import com.charliesbot.onewearos.presentation.data.repositories.WearMessageRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val wearAppModule = module {
    viewModelOf(::WearTodayViewModel)
    factoryOf(::WearMessageRepository)
}