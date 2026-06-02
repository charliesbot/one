package com.charliesbot.shared.core.di

import androidx.room.Room
import com.charliesbot.shared.core.data.db.AppDatabase
import com.charliesbot.shared.core.data.history.FastingHistoryRepositoryImpl
import com.charliesbot.shared.core.domain.repository.FastingHistoryRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val historyDatabaseModule = module {
  single {
    Room.databaseBuilder(androidContext(), AppDatabase::class.java, "fasting_history.db")
      .apply {
        if (com.charliesbot.shared.core.data.BuildConfig.DEBUG) {
          fallbackToDestructiveMigration(true)
        }
      }
      .build()
  }

  single { get<AppDatabase>().fastingRecordDao() }

  single<FastingHistoryRepository> { FastingHistoryRepositoryImpl(fastingRecordDao = get()) }
}
