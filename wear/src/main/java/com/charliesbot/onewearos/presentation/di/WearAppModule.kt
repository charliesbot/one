package com.charliesbot.onewearos.presentation.di

import com.charliesbot.one.widget.wear.WearWidgetUpdateManager
import com.charliesbot.onewearos.complications.ComplicationUpdateManager
import com.charliesbot.onewearos.presentation.data.WearStringProvider
import com.charliesbot.onewearos.presentation.notifications.NotificationWorker
import com.charliesbot.onewearos.presentation.notifications.OngoingActivityManager
import com.charliesbot.onewearos.presentation.services.LocalWatchFastingCallbacks
import com.charliesbot.shared.core.data.notifications.NotificationScheduler
import com.charliesbot.shared.core.domain.events.FastingEventCallbacks
import com.charliesbot.shared.core.domain.notifications.FastingNotificationScheduler
import com.charliesbot.shared.core.domain.platform.StringProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val wearAppModule = module {
  single<NotificationScheduler> {
    NotificationScheduler(
      context = androidContext(),
      workerClass = NotificationWorker::class.java,
      settingsRepository = get(),
    )
  }
  single<FastingNotificationScheduler> { get<NotificationScheduler>() }
  single<StringProvider> { WearStringProvider(androidContext()) }
  single<ComplicationUpdateManager> { ComplicationUpdateManager(androidContext()) }
  single<WearWidgetUpdateManager> { WearWidgetUpdateManager(androidContext()) }
  single<OngoingActivityManager> {
    OngoingActivityManager(context = androidContext(), fastingDataRepository = get())
  }
  single { LocalWatchFastingCallbacks(get(), get(), get(), get()) }
  single<FastingEventCallbacks> { get<LocalWatchFastingCallbacks>() }
}
