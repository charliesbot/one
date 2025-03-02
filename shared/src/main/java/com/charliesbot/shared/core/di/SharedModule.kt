package com.charliesbot.shared.core.di

import com.charliesbot.shared.core.datalayer.FastingDataClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val sharedModule = module {
    single<FastingDataClient> { FastingDataClient(androidContext()) }
}