package com.charliesbot.shared.core.di

import androidx.datastore.core.DataStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import androidx.datastore.preferences.core.Preferences
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepositoryImpl
import com.charliesbot.shared.core.datastore.fastingDataStore

val sharedModule = module {
    single<DataStore<Preferences>> { androidContext().fastingDataStore }
    single<FastingDataRepository> {
        FastingDataRepositoryImpl(
            androidContext(),
            dataStore = get()
        )
    }
}