package com.charliesbot.shared.core.di

import androidx.datastore.core.DataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import androidx.datastore.preferences.core.Preferences
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepositoryImpl
import com.charliesbot.shared.core.datastore.fastingDataStore
import com.charliesbot.shared.core.domain.usecase.FastingUseCase
import com.charliesbot.shared.core.services.FastingEventManager

val sharedModule = module {
    single<DataStore<Preferences>> { androidContext().fastingDataStore }
    single<FastingDataRepository> {
        FastingDataRepositoryImpl(
            androidContext(),
            dataStore = get()
        )
    }
    single<FastingEventManager> { FastingEventManager() }
    single { FastingUseCase(get(), get(), get()) }
}