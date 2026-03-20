package com.charliesbot.shared.core.domain.model

import androidx.compose.ui.graphics.Color
import com.charliesbot.shared.core.constants.FastGoal
import kotlinx.serialization.Serializable

@Serializable
data class CustomGoalData(val id: String, val name: String, val durationMillis: Long, val colorHex: Long)

internal fun CustomGoalData.toFastGoal(): FastGoal = FastGoal(
    id = id,
    titleText = name,
    durationDisplay = formatDurationDisplay(durationMillis),
    color = Color(colorHex.toULong()),
    durationMillis = durationMillis,
)

internal fun FastGoal.toData(): CustomGoalData = CustomGoalData(
    id = id,
    name = titleText ?: id,
    durationMillis = durationMillis,
    colorHex = color.value.toLong(),
)

internal fun formatDurationDisplay(millis: Long): String {
    val totalHours = millis / (60L * 60L * 1000L)
    return totalHours.toString()
}
