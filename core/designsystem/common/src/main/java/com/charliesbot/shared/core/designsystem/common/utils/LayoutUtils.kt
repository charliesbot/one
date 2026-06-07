package com.charliesbot.shared.core.designsystem.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.window.core.layout.WindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.layout.adapter.computeWindowSizeClass

@Composable
fun calculateWindowSizeClass(): WindowSizeClass {
  val currentWindowMetrics =
    WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(LocalContext.current)
  return WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(currentWindowMetrics)
}

@Composable
fun isWidthAtLeastMedium(): Boolean {
  val sizeClass = calculateWindowSizeClass()
  return sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
}

@Composable
fun areBothWindowDimensionsAtLeastMedium(): Boolean {
  val sizeClass = calculateWindowSizeClass()
  return sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) &&
    sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
}
