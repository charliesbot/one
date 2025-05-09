package com.charliesbot.onewearos.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun OneTheme(
    content: @Composable () -> Unit
) {
    // TODO (charliesbot): Uncomment when material theme for WearOS is ready
    //    val context = LocalContext.current
    //    val colorScheme = dynamicColorScheme(context)
    val colorScheme: ColorScheme =
        MaterialTheme.colorScheme.copy(
            primary = Color(0xFF4ED07D), // The green color from your image
            primaryContainer =
                Color(0xFF3DBC6C) // Slightly darker variant
        )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}