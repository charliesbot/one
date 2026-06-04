package com.charliesbot.shared.core.utils

import androidx.compose.ui.graphics.Color
import com.charliesbot.shared.core.constants.FastGoal
import com.charliesbot.shared.core.models.CustomGoalData

fun CustomGoalData.toFastGoal(): FastGoal =
  FastGoal(
    id = id,
    titleText = name,
    durationDisplay = formatDurationDisplay(durationMillis),
    color = Color(colorHex.toULong()),
    durationMillis = durationMillis,
  )

fun FastGoal.toData(): CustomGoalData =
  CustomGoalData(
    id = id,
    name = titleText ?: id,
    durationMillis = durationMillis,
    colorHex = color.value.toLong(),
  )

private fun formatDurationDisplay(millis: Long): String {
  val totalHours = millis / (60L * 60L * 1000L)
  return totalHours.toString()
}
