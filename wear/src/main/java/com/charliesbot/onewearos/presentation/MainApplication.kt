package com.charliesbot.onewearos.presentation

import android.app.Application
import android.util.Log
import androidx.wear.phone.interactions.notifications.BridgingConfig
import androidx.wear.phone.interactions.notifications.BridgingManager
import androidx.work.Configuration
import com.charliesbot.one.widget.wear.WearWidgetRefreshScheduler
import com.charliesbot.onewearos.di.wearDashboardModule
import com.charliesbot.onewearos.presentation.di.wearAppModule
import com.charliesbot.shared.core.data.notifications.NotificationUtil
import com.charliesbot.shared.core.di.sharedModule
import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.utils.GoalResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class MainApplication : Application(), Configuration.Provider {
  override fun onCreate() {
    super.onCreate()

    disableNotificationBridge()

    // Create notification channel early so smart reminders can use it
    NotificationUtil.createNotificationChannel(this)

    startKoin {
      androidLogger()
      androidContext(this@MainApplication)
      modules(sharedModule, wearAppModule, wearDashboardModule)
    }

    // Reconcile widget refresh schedule on launch if fast is active
    CoroutineScope(Dispatchers.IO).launch {
      val fastingData = get<FastingDataRepository>().getCurrentFasting()
      if (fastingData?.isFasting == true) {
        val duration = get<GoalResolver>().resolveGoalDurationMillis(fastingData.fastingGoalId)
        get<WearWidgetRefreshScheduler>().scheduleNext(
          startTimeMillis = fastingData.startTimeInMillis,
          goalDurationMillis = duration,
        )
      }
    }
  }

  private fun disableNotificationBridge() {
    try {
      val config = BridgingConfig.Builder(this, false).build()
      BridgingManager.fromContext(this).setConfig(config)
      Log.d("WatchApplication", "Notification bridging disabled successfully.")
    } catch (e: Exception) {
      Log.e("WatchApplication", "Failed to disable notification bridging", e)
    }
  }

  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().build()
}
