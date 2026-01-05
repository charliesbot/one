package com.charliesbot.one.widgets

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

internal object OneWidgetSize {
    // 1x1: Progress bar only
    val SMALL_SQUARE = DpSize(width = 57.dp, height = 57.dp)
    // Nx1: Horizontal layout with progress + text
    val HORIZONTAL_RECTANGLE = DpSize(width = 130.dp, height = 57.dp)
    // 2x2+: Vertical layout with progress + text
    val BIG_SQUARE = DpSize(width = 130.dp, height = 130.dp)
}