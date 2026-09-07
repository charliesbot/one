package com.charliesbot.one.di

import android.content.Context
import android.content.SharedPreferences
import com.charliesbot.one.data.AndroidAppVersionProvider
import com.charliesbot.one.data.AndroidClipboardHelper
import com.charliesbot.one.data.AndroidHistoryExporter
import com.charliesbot.one.data.AndroidStringProvider
import com.charliesbot.one.notifications.NotificationWorker
import com.charliesbot.one.services.LocalFastingCallback
import com.charliesbot.one.services.SmartReminderCallbackImpl
import com.charliesbot.one.widget.PhoneWidgetHost
import com.charliesbot.one.widget.WidgetUpdateManager
import com.charliesbot.one.widget.common.GoalDurationResolver
import com.charliesbot.one.widget.common.WidgetPlatformAdapter
import com.charliesbot.one.widget.common.WidgetRefreshScheduler
import com.charliesbot.one.widget.work.WorkManagerWidgetAdapter
import com.charliesbot.shared.core.data.notifications.NotificationScheduler
import com.charliesbot.shared.core.domain.events.FastingEventCallbacks
import com.charliesbot.shared.core.domain.notifications.FastingNotificationScheduler
import com.charliesbot.shared.core.domain.platform.AppVersionProvider
import com.charliesbot.shared.core.domain.platform.ClipboardHelper
import com.charliesbot.shared.core.domain.platform.HistoryExporter
import com.charliesbot.shared.core.domain.platform.SmartReminderCallback
import com.charliesbot.shared.core.domain.platform.StringProvider
import com.charliesbot.shared.core.domain.usecase.GetMonthlyFastingMapUseCase
import com.charliesbot.shared.core.utils.GoalResolver
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
  single<SharedPreferences> {
    androidContext().getSharedPreferences("one_fasting_prefs", Context.MODE_PRIVATE)
  }

  single<WidgetUpdateManager> { WidgetUpdateManager(androidContext()) }
  single<WidgetPlatformAdapter> {
    WorkManagerWidgetAdapter(
      context = androidContext(),
      workName = "fasting_phone_widget_hourly_refresh",
      host = PhoneWidgetHost(androidContext()),
    )
  }
  single {
    WidgetRefreshScheduler(
      fastingDataRepository = get(),
      goalDurationResolver =
        GoalDurationResolver { goalId -> get<GoalResolver>().durationMillis(goalId) },
      platformAdapter = get(),
    )
  }

  single<NotificationScheduler> {
    NotificationScheduler(
      context = androidContext(),
      workerClass = NotificationWorker::class.java,
      settingsRepository = get(),
    )
  }
  single<FastingNotificationScheduler> { get<NotificationScheduler>() }

  single<StringProvider> { AndroidStringProvider(androidContext()) }

  single<AppVersionProvider> { AndroidAppVersionProvider(androidContext()) }

  single<HistoryExporter> { AndroidHistoryExporter(androidContext()) }

  single<ClipboardHelper> { AndroidClipboardHelper(androidContext()) }

  factory { GetMonthlyFastingMapUseCase(get()) }

  single { LocalFastingCallback(get(), get(), get()) }
  single<FastingEventCallbacks> { get<LocalFastingCallback>() }

  single<SmartReminderCallback> { SmartReminderCallbackImpl(androidContext()) }
}
