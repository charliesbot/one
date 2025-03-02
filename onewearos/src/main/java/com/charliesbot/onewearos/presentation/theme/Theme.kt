package com.charliesbot.onewearos.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.MaterialTheme

@Composable
fun OneTheme(
    content: @Composable () -> Unit
) {
    /**
     * Empty theme to customize for your app.
     * See: https://developer.android.com/jetpack/compose/designsystems/custom
     */
    val defaultColors = MaterialTheme.colors
    val updatedColors = defaultColors.copy(
        primary = Color(0xFF4ED07D), // The green color from your image
        primaryVariant = Color(0xFF3DBC6C) // Slightly darker variant
    )
    MaterialTheme(
        colors = updatedColors,
        content = content
    )
}