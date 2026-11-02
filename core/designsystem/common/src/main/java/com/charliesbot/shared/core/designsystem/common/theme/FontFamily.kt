package com.charliesbot.shared.core.designsystem.common.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.charliesbot.shared.core.designsystem.common.R

fun ascenderHeight(ascenderHeight: Float): FontVariation.Setting {
  require(ascenderHeight in 649f..854f) { "'Ascender Height' must be in 649f..854f" }
  return FontVariation.Setting("YTAS", ascenderHeight)
}

fun counterWidth(counterWidth: Int): FontVariation.Setting {
  require(counterWidth in 323..603) { "'Counter width' must be in 323..603" }
  return FontVariation.Setting("XTRA", counterWidth.toFloat())
}

fun grade(grade: Float): FontVariation.Setting {
  require(grade in -200f..150f) { "'Grade' must be in -200f..150f" }
  return FontVariation.Setting("GRAD", grade)
}

fun thickStroke(value: Float): FontVariation.Setting = FontVariation.Setting("XTRA", value)

fun thinStroke(value: Float): FontVariation.Setting = FontVariation.Setting("YOPQ", value)

fun roundness(roundness: Float): FontVariation.Setting {
  require(roundness in 0f..100f) { "'Roundness' must be in 0f..100f" }
  return FontVariation.Setting("ROND", roundness)
}

@OptIn(ExperimentalTextApi::class)
val GoogleSans =
  FontFamily(
    Font(
      resId = R.font.google_sans_flex_variable,
      variationSettings =
        FontVariation.Settings(
          FontVariation.weight(FontWeight.Normal.weight),
          FontVariation.width(100f),
          FontVariation.slant(0f),
        ),
    )
  )

@OptIn(ExperimentalTextApi::class)
val GoogleSansWide =
  FontFamily(
    Font(
      resId = R.font.google_sans_flex_variable,
      variationSettings =
        FontVariation.Settings(
          FontVariation.weight(FontWeight.Normal.weight),
          FontVariation.width(125f),
          FontVariation.slant(0f),
          counterWidth(405),
        ),
    )
  )

@OptIn(ExperimentalTextApi::class)
val GoogleSansTitleLargeSpecific =
  FontFamily(
    Font(
      resId = R.font.google_sans_flex_variable,
      variationSettings =
        FontVariation.Settings(
          FontVariation.weight(761),
          FontVariation.width(130.5f),
          FontVariation.slant(0f),
          grade(-108f),
          thickStroke(146f),
          thinStroke(57f),
        ),
    )
  )

/**
 * Bold, wide and fully rounded: display titles such as the Today screen header. [opticalSize] is
 * the text size it is drawn at, so the letterforms are tuned for that size.
 */
@OptIn(ExperimentalTextApi::class)
fun googleSansRoundedWide(opticalSize: TextUnit): FontFamily =
  FontFamily(
    Font(
      resId = R.font.google_sans_flex_variable,
      variationSettings =
        FontVariation.Settings(
          FontVariation.weight(760),
          FontVariation.width(125f),
          FontVariation.slant(0f),
          FontVariation.opticalSizing(opticalSize),
          roundness(100f),
        ),
    )
  )

/**
 * Heavy, narrow and fully rounded: large numerals such as clock times and timers. [opticalSize] is
 * the text size it is drawn at, so the letterforms are tuned for that size.
 */
@OptIn(ExperimentalTextApi::class)
fun googleSansRoundedNarrow(opticalSize: TextUnit): FontFamily =
  FontFamily(
    Font(
      resId = R.font.google_sans_flex_variable,
      variationSettings =
        FontVariation.Settings(
          FontVariation.weight(800),
          FontVariation.width(75f),
          FontVariation.slant(0f),
          FontVariation.opticalSizing(opticalSize),
          roundness(100f),
        ),
    )
  )
