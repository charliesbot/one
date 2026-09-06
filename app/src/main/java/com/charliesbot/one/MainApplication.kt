package com.charliesbot.one

import android.app.Application
import com.charliesbot.one.di.appModule
import com.charliesbot.one.features.dashboard.di.dashboardModule
import com.charliesbot.one.features.profile.di.profileModule
import com.charliesbot.one.features.settings.di.settingsModule
import com.charliesbot.one.notifications.SmartReminderWorker
import com.charliesbot.one.widget.PhoneWidgetRefreshScheduler
import com.charliesbot.shared.core.di.historyDatabaseModule
import com.charliesbot.shared.core.di.sharedModule
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.utils.GoalResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    startKoin {
      // Log Koin into Android logger
      androidLogger()
      // Reference Android context
      androidContext(this@MainApplication)
      // Load modules
      modules(
        sharedModule,
        historyDatabaseModule,
        appModule,
        dashboardModule,
        profileModule,
        settingsModule,
      )
    }

    // Schedule the daily smart reminder worker
    // The worker itself checks if smart reminders are enabled before executing
    SmartReminderWorker.scheduleDailyWorker(this)

    // Reconcile widget refresh schedule on launch if fast is active
    CoroutineScope(Dispatchers.IO).launch {
      val fastingData = get<FastingDataRepository>().getCurrentFasting()
      if (fastingData?.isFasting == true) {
        val duration = get<GoalResolver>().resolveGoalDurationMillis(fastingData.fastingGoalId)
        get<PhoneWidgetRefreshScheduler>().scheduleNext(
          startTimeMillis = fastingData.startTimeInMillis,
          goalDurationMillis = duration,
        )
      }
    }
  }
}
