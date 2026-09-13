package com.charliesbot.shared.core.designsystem.common.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import com.charliesbot.shared.core.models.ClockFormat
import java.util.Locale

val LocalClockFormat = compositionLocalOf { ClockFormat(false, Locale.getDefault()) }

@Composable
fun ClockFormatProvider(is24Hour: Boolean, content: @Composable () -> Unit) {
  val locale = LocalConfiguration.current.locales[0]
  CompositionLocalProvider(
    LocalClockFormat provides ClockFormat(is24Hour, locale),
    content = content,
  )
}
