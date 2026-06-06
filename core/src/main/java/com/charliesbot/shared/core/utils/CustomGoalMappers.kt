package com.charliesbot.shared.core.utils

import androidx.compose.ui.graphics.Color
import com.charliesbot.shared.core.designsystem.common.goals.FastGoal
import com.charliesbot.shared.core.designsystem.common.goals.PredefinedFastingGoals
import com.charliesbot.shared.core.models.CustomGoalData
import com.charliesbot.shared.core.models.FastingGoal

fun CustomGoalData.toFastGoal(): FastGoal =
  FastGoal(
    id = id,
    titleText = name,
    durationDisplay = formatDurationDisplay(durationMillis),
    color = Color(colorHex.toULong()),
    durationMillis = durationMillis,
  )

fun FastingGoal.toFastGoal(): FastGoal =
  if (isCustom) {
    FastGoal(
      id = id,
      titleText = name ?: id,
      durationDisplay = durationDisplay,
      color = Color(requireNotNull(colorHex).toULong()),
      durationMillis = durationMillis,
    )
  } else {
    PredefinedFastingGoals.getGoalById(id)
  }

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
