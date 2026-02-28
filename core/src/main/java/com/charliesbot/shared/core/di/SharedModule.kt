package com.charliesbot.shared.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.charliesbot.shared.core.data.repositories.customGoalRepository.CustomGoalRepository
import com.charliesbot.shared.core.data.repositories.customGoalRepository.CustomGoalRepositoryImpl
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepositoryImpl
import com.charliesbot.shared.core.data.repositories.settingsRepository.SettingsRepository
import com.charliesbot.shared.core.data.repositories.settingsRepository.SettingsRepositoryImpl
import com.charliesbot.shared.core.datastore.fastingDataStore
import com.charliesbot.shared.core.domain.usecase.FastingUseCase
import com.charliesbot.shared.core.domain.usecase.GetSuggestedFastingStartTimeUseCase
import com.charliesbot.shared.core.services.FastingEventManager
import com.charliesbot.shared.core.utils.GoalResolver
import com.google.android.gms.wearable.Wearable
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val sharedModule = module {
    single<DataStore<Preferences>> { androidContext().fastingDataStore }
    single<FastingDataRepository> {
        FastingDataRepositoryImpl(
            androidContext(),
            dataStore = get(),
        )
    }
    single<SettingsRepository> {
        SettingsRepositoryImpl(
            androidContext(),
            dataStore = get(),
        )
    }
    single<FastingEventManager> { FastingEventManager() }
    single { FastingUseCase(get(), get(), get()) }
    single { GetSuggestedFastingStartTimeUseCase(get(), get()) }

    single<CustomGoalRepository> { CustomGoalRepositoryImpl(androidContext(), dataStore = get()) }
    single { GoalResolver(get()) }

    single { Wearable.getDataClient(androidContext()) }
    single { Wearable.getMessageClient(androidContext()) }
    single { Wearable.getCapabilityClient(androidContext()) }
    single { Wearable.getNodeClient(androidContext()) }
}
