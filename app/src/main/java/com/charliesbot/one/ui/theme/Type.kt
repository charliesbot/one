package com.charliesbot.one.ui.theme

import android.os.Build
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

// Assuming "Thick Stroke" and "Thin Stroke" map to XTRA (Parametric Counter Width) and YOPQ (Optical Parametric Thinness) respectively,
// or are custom axes that need specific tags.
// These ranges are illustrative and depend on the font's actual capabilities.
fun thickStroke(value: Float): FontVariation.Setting {
    // This tag is a guess, based on common variable font axes related to stroke or parametric design.
    // The actual tag might be "SHRP", "OPQT", "CNTR", or something font-specific.
    // For Google Sans Flex, if it's not XTRA, it might be a custom axis.
    return FontVariation.Setting("XTRA", value) // Re-using XTRA as a placeholder for thick stroke
}

fun thinStroke(value: Float): FontVariation.Setting {
    // This tag is a guess for a parametric thinness axis.
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
                FontVariation.width(125f), // Increased width for "wide" look
                FontVariation.slant(0f),
                counterWidth(405) // Using an intermediate value for general wide styles
            )
        )
    )


@OptIn(ExperimentalTextApi::class)
val GoogleSansTitleLargeSpecific =
    FontFamily(
        Font(
            resId = R.font.google_sans_flex_variable,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(761), // Specific weight
                FontVariation.width(130.5f), // Specific width
                FontVariation.slant(0f),
                grade(-108f), // Specific grade
                thickStroke(146f), // Specific thick stroke
                thinStroke(57f) // Specific thin stroke
            )
        )
    )


// Material 3 Typography Scale
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = GoogleSansWide,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = GoogleSansWide,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = GoogleSansWide,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = GoogleSansWide,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = GoogleSansWide,
        fontWeight = FontWeight(621),
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = GoogleSansTitleLargeSpecific, // Using the new specific FontFamily
        fontWeight = FontWeight(761), // Applied here as well for clarity and explicit weight
        fontSize = 28.sp, // Specific size
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)