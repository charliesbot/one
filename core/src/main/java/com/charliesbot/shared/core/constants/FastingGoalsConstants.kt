package com.charliesbot.shared.core.constants

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.charliesbot.shared.R

data class FastGoal(
  val id: String,
  val titleResId: Int = 0,
  val titleText: String? = null,
  val durationDisplay: String, // e.g., "13", "16" (for display in UI)
  val color: Color,
  val durationMillis: Long, // The actual duration in milliseconds for calculations
) {
  val isCustom: Boolean
    get() = id.startsWith("custom_")

  fun getTitle(context: android.content.Context): String =
    titleText ?: context.getString(titleResId)
}

object PredefinedFastingGoals {

  // Minimum hours to consider a completed fast
  const val MIN_FASTING_HOURS = 13f

  // Helper to calculate milliseconds from hours
  private fun hoursToMillis(hours: Int): Long = hours * 60L * 60L * 1000L

  val CIRCADIAN =
    FastGoal(
      id = "circadian",
      titleResId = R.string.fasting_goal_circadian,
      durationDisplay = "13",
      color = Color(0xFF6096BA), // Dusty Blue / Muted Teal
      durationMillis = hoursToMillis(13),
    )

  val SIXTEEN_EIGHT =
    FastGoal(
      id = "16:8",
      titleResId = R.string.fasting_goal_16_8,
      durationDisplay = "16",
      color = Color(0xFF82B387), // Muted Sage Green
      durationMillis = hoursToMillis(16),
    )

  val EIGHTEEN_SIX =
    FastGoal(
      id = "18:6",
      titleResId = R.string.fasting_goal_18_6,
      durationDisplay = "18",
      color = Color(0xFFE5A98C), // Muted Peach / Terracotta
      durationMillis = hoursToMillis(18),
    )

  val TWENTY_FOUR =
    FastGoal(
      id = "20:4",
      titleResId = R.string.fasting_goal_20_4,
      durationDisplay = "20",
      color = Color(0xFFC9AB6A), // Muted Gold / Ochre
      durationMillis = hoursToMillis(20),
    )

  val THIRTY_SIX_HOUR =
    FastGoal(
      id = "36hour",
      titleResId = R.string.fasting_goal_36_hour,
      durationDisplay = "36",
      color = Color(0xFF9787BE), // Dusty Lavender / Muted Plum
      durationMillis = hoursToMillis(36),
    )

  val allGoals: List<FastGoal> =
    listOf(CIRCADIAN, SIXTEEN_EIGHT, EIGHTEEN_SIX, TWENTY_FOUR, THIRTY_SIX_HOUR)

  val goalsById: Map<String, FastGoal> = allGoals.associateBy { it.id }

  val customGoalColors: List<Color> =
    listOf(
      Color(0xFFB07AA1), // Muted Mauve
      Color(0xFF7BA3A8), // Teal Slate
      Color(0xFFD4956B), // Warm Amber
      Color(0xFF8A9A5B), // Olive Green
      Color(0xFF9E8B70), // Taupe
      Color(0xFF6C8EBF), // Steel Blue
    )

  @Volatile private var customGoalsMap: Map<String, FastGoal> = emptyMap()

  fun registerCustomGoals(goals: List<FastGoal>) {
    customGoalsMap = goals.associateBy { it.id }
  }

  val getGoalById: (String) -> FastGoal = { id: String ->
    val predefined = goalsById[id]
    val custom = customGoalsMap[id]
    when {
      predefined != null -> predefined

      custom != null -> custom

      else -> {
        Log.e(AppConstants.LOG_TAG, "Invalid goal id: $id")
        goalsById["16:8"]!!
      }
    }
  }
}
