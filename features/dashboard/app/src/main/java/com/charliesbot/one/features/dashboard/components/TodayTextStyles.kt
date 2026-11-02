package com.charliesbot.one.features.dashboard.components

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.charliesbot.shared.core.designsystem.common.theme.googleSansRoundedNarrow
import com.charliesbot.shared.core.designsystem.common.theme.googleSansRoundedWide

/** The Today screen's display voices; everything smaller uses the theme's type scale. */
internal object TodayTextStyles {
  val ScreenTitle =
    TextStyle(fontFamily = googleSansRoundedWide(36.sp), fontSize = 36.sp, lineHeight = 48.sp)

  /** The idle card's start or finish time. */
  val HeroClock =
    TextStyle(
      fontFamily = googleSansRoundedNarrow(96.sp),
      fontSize = 96.sp,
      lineHeight = 120.sp,
      letterSpacing = (-2).sp,
      fontFeatureSettings = "tnum",
    )

  /** The elapsed time inside the fast dial, sized to leave clear room inside the arc. */
  val DialClock =
    TextStyle(
      fontFamily = googleSansRoundedNarrow(56.sp),
      fontSize = 56.sp,
      lineHeight = 72.sp,
      letterSpacing = (-2).sp,
      fontFeatureSettings = "tnum",
    )

  /** The time on a Started or Goal tile. */
  val TileValue =
    TextStyle(
      fontFamily = googleSansRoundedNarrow(28.sp),
      fontSize = 28.sp,
      lineHeight = 36.sp,
      fontFeatureSettings = "tnum",
    )
}
