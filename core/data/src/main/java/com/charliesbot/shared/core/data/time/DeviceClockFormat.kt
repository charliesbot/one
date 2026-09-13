package com.charliesbot.shared.core.data.time

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateFormat
import com.charliesbot.shared.core.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/** Observes the device preference only while a UI is collecting. */
class DeviceClockFormat(context: Context, settings: SettingsRepository) {
  private val context = context.applicationContext
  private val system24Hour = callbackFlow {
    val observer =
      object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
          trySend(DateFormat.is24HourFormat(this@DeviceClockFormat.context))
        }
      }
    val resolver = this@DeviceClockFormat.context.contentResolver
    resolver.registerContentObserver(
      Settings.System.getUriFor(Settings.System.TIME_12_24),
      false,
      observer,
    )
    trySend(DateFormat.is24HourFormat(this@DeviceClockFormat.context))
    awaitClose { resolver.unregisterContentObserver(observer) }
  }
  val is24Hour =
    combine(settings.timeFormatMode, system24Hour) { mode, system -> mode.is24Hour(system) }
      .distinctUntilChanged()
}
