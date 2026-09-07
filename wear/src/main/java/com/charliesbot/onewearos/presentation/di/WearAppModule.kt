package com.charliesbot.onewearos.presentation.di

import com.charliesbot.one.widget.common.GoalDurationResolver
import com.charliesbot.one.widget.common.WidgetPlatformAdapter
import com.charliesbot.one.widget.common.WidgetRefreshScheduler
import com.charliesbot.one.widget.wear.WearWidgetPlatformAdapter
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
import com.charliesbot.shared.core.utils.GoalResolver
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
  single<WidgetPlatformAdapter> { WearWidgetPlatformAdapter(androidContext()) }
  single {
    WidgetRefreshScheduler(
      fastingDataRepository = get(),
      goalDurationResolver =
        GoalDurationResolver { goalId -> get<GoalResolver>().durationMillis(goalId) },
      platformAdapter = get(),
    )
  }
  single<OngoingActivityManager> {
    OngoingActivityManager(context = androidContext(), fastingDataRepository = get())
  }
  single { LocalWatchFastingCallbacks(get(), get(), get(), get()) }
  single<FastingEventCallbacks> { get<LocalWatchFastingCallbacks>() }
}
