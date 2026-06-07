package com.charliesbot.shared.core.domain.goals

import com.charliesbot.shared.core.models.CustomGoalData
import com.charliesbot.shared.core.models.FastingGoal

fun CustomGoalData.toFastingGoal(): FastingGoal =
  FastingGoal(id = id, name = name, durationMillis = durationMillis, colorHex = colorHex)
