package com.charliesbot.onewearos.complications

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.charliesbot.shared.core.updates.DebouncedUpdateRequester
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class ComplicationUpdateManager(
  private val applicationContext: Context,
  timeWindow: Long = 1000L,
  coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
  private val requester =
    DebouncedUpdateRequester(
      name = "ComplicationUpdateManager",
      timeWindow = timeWindow,
      coroutineDispatcher = coroutineDispatcher,
      update = {
        ComplicationDataSourceUpdateRequester.create(
            applicationContext,
            ComponentName(applicationContext, MainComplicationService::class.java),
          )
          .requestUpdateAll()
      },
    )

  fun requestUpdate() = requester.requestUpdate()

  fun cancel() = requester.cancel()
}
