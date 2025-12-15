package com.charliesbot.shared.core.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.charliesbot.shared.R

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

fun thickStroke(value: Float): FontVariation.Setting {
    return FontVariation.Setting("XTRA", value)
}

fun thinStroke(value: Float): FontVariation.Setting {
    return FontVariation.Setting("YOPQ", value)
}

@OptIn(ExperimentalTextApi::class)
val GoogleSans =
    FontFamily(
        Font(
            resId = R.font.google_sans_flex_variable,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(FontWeight.Normal.weight),
                FontVariation.width(100f),
                FontVariation.slant(0f),
            )
        )
    )

@OptIn(ExperimentalTextApi::class)
val GoogleSansWide =
    FontFamily(
        Font(
            resId = R.font.google_sans_flex_variable,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(FontWeight.Normal.weight),
                FontVariation.width(125f),
                FontVariation.slant(0f),
                counterWidth(405)
            )
        )
    )

@OptIn(ExperimentalTextApi::class)
val GoogleSansTitleLargeSpecific =
    FontFamily(
        Font(
            resId = R.font.google_sans_flex_variable,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(761),
                FontVariation.width(130.5f),
                FontVariation.slant(0f),
                grade(-108f),
                thickStroke(146f),
                thinStroke(57f)
            )
        )
    )
