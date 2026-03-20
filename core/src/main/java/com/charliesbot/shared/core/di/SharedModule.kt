package com.charliesbot.shared.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.charliesbot.shared.core.data.repository.CustomGoalRepositoryImpl
import com.charliesbot.shared.core.data.repository.FastingDataRepositoryImpl
import com.charliesbot.shared.core.data.repository.SettingsRepositoryImpl
import com.charliesbot.shared.core.datastore.fastingDataStore
import com.charliesbot.shared.core.domain.repository.CustomGoalRepository
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.domain.repository.SettingsRepository
import com.charliesbot.shared.core.domain.usecase.GetSuggestedFastingStartTimeUseCase
import com.charliesbot.shared.core.domain.usecase.ObserveFastingStateUseCase
import com.charliesbot.shared.core.domain.usecase.StartFastingUseCase
import com.charliesbot.shared.core.domain.usecase.StopFastingUseCase
import com.charliesbot.shared.core.domain.usecase.SyncFastingStateUseCase
import com.charliesbot.shared.core.domain.usecase.UpdateFastingConfigUseCase
import com.charliesbot.shared.core.services.FastingEventManager
import com.charliesbot.shared.core.utils.GoalResolver
import com.google.android.gms.wearable.Wearable
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val sharedModule = module {
  single<DataStore<Preferences>> { androidContext().fastingDataStore }
  single<FastingDataRepository> { FastingDataRepositoryImpl(androidContext(), dataStore = get()) }
  single<SettingsRepository> { SettingsRepositoryImpl(androidContext(), dataStore = get()) }
  single<FastingEventManager> { FastingEventManager() }
  single { GetSuggestedFastingStartTimeUseCase(get(), get()) }

  factory { ObserveFastingStateUseCase(get()) }
  factory { StartFastingUseCase(get(), get(), get()) }
  factory { StopFastingUseCase(get(), get(), get()) }
  factory { UpdateFastingConfigUseCase(get(), get(), get()) }
  factory { SyncFastingStateUseCase(get()) }

  single<CustomGoalRepository> { CustomGoalRepositoryImpl(androidContext(), dataStore = get()) }
  single { GoalResolver(get()) }

  single { Wearable.getDataClient(androidContext()) }
  single { Wearable.getMessageClient(androidContext()) }
  single { Wearable.getCapabilityClient(androidContext()) }
  single { Wearable.getNodeClient(androidContext()) }
}
